import OpenSubtitlesHasher
import groovy.util.logging.Log4j
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.FileAppender
import org.apache.log4j.TTCCLayout
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

import java.util.zip.GZIPInputStream

@Log4j
class ArchimedesClient {

    final String ELL_LANG = "ell"

    final String OPEN_SUBTITLES_URL = "http://api.opensubtitles.org/xml-rpc"

    final String UTF8_BOM = "\uFEFF"

    XmlRpcClient client

    String token

    ArchimedesClient() {
        log.addAppender(new ConsoleAppender(new TTCCLayout()))
        log.addAppender(new FileAppender(new TTCCLayout(), 'eureka-subtitles-report.log', false))

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl()
        config.setServerURL(new URL(OPEN_SUBTITLES_URL))

        client = new XmlRpcClient()
        client.setConfig(config)

        token = login()
    }

    String login() {
        Object[] params = ["", "", "eng", "moviejukebox 1.0.15"]
        def result = client.execute("LogIn", params)
        result["token"]
    }

    void logout() {
        client.execute("LogOut", [token])
    }

    void discoverSubtitles(File movieFile) {
        log.info movieFile.name

        def movieFilepath = movieFile.absolutePath

        if (getSubtitleFile(movieFilepath).exists()) {
            log.info "Subtitle already there\n"
            return
        }

        def movieHash = OpenSubtitlesHasher.computeHash(movieFilepath)
        def params = [sublanguageid: ELL_LANG, moviehash: movieHash]
        def response = client.execute("SearchSubtitles", [token, [params]])

        try {
            if (response["data"].size() == 0) {
                log.error "No subtitles found\n"
                return
            }

            def entry = response["data"].first()

            String downloadUrl = entry["SubDownloadLink"]
            String encoding = entry["SubEncoding"]

            log.info "Eureka!"
            log.info "url: ${downloadUrl} [encoding: ${encoding}]\n"

            getSubtitleFile(movieFilepath).withWriter('UTF-8') {
                InputStream subStream = new URL(downloadUrl).openStream()
                String subsText = new GZIPInputStream(subStream).getText(encoding)
                if (!subsText.startsWith(UTF8_BOM)) {
                    it << UTF8_BOM
                }
                it << subsText
            }

        } catch (Exception ex) {
            log.debug(response, ex)
        }
    }

    static File getSubtitleFile(String movieFilepath) {
        String fileWithoutExt = movieFilepath.take(movieFilepath.lastIndexOf('.'))
        return new File(fileWithoutExt + ".srt")
    }
}
