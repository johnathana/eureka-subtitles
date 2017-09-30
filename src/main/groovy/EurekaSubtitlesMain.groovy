import groovy.io.FileType

class EurekaSubtitlesMain {

    static void main(String[] args) {

        ArchimedesClient archimedes = new ArchimedesClient()

        new File('.').eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('.mp4') ||
                    it.name.endsWith('.mkv') ||
                    it.name.endsWith('.avi')) {
                println it
                archimedes.discoverSubtitles(it.absolutePath)
            }
        }

        archimedes.logout()
    }
}
