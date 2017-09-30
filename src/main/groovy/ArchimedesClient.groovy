import OpenSubtitlesHasher
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

import java.util.zip.GZIPInputStream

class ArchimedesClient {

    final String ELL_LANG = "ell"

    final String OPEN_SUBTITLES_URL = "http://api.opensubtitles.org/xml-rpc"

    final String UTF8_BOM = "\uFEFF";

    XmlRpcClient client

    String token

    ArchimedesClient() {
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

    void discoverSubtitles(String movieFilepath) {

        if (getSubtitleFile(movieFilepath).exists()) {
            println "Subtitle already there."
            return
        }

        def movieHash = OpenSubtitlesHasher.computeHash(movieFilepath)
        println "hash: " + movieHash

        def params = [sublanguageid: ELL_LANG, moviehash: movieHash]
        def response = client.execute("SearchSubtitles", [token, [params]])

        try {
            def entry = response["data"].first()

            String downloadUrl = entry["SubDownloadLink"]
            String encoding = entry["SubEncoding"]

            println "url: " + downloadUrl
            println "encoding: " + encoding

            getSubtitleFile(movieFilepath).withWriter('UTF-8') {
                InputStream subStream = new URL(downloadUrl).openStream()
                String subsText = new GZIPInputStream(subStream).getText(encoding)
                if (!subsText.startsWith(UTF8_BOM)) {
                    it << UTF8_BOM
                }
                it << subsText
            }

            println("Eureka!")

        } catch (Exception ex) {
            println response
            ex.printStackTrace()
        }
    }

    static File getSubtitleFile(String movieFilepath) {
        String fileWithoutExt = movieFilepath.take(movieFilepath.lastIndexOf('.'))
        return new File(fileWithoutExt + ".srt")
    }
}
