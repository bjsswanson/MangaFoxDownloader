@Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14') 
import org.cyberneko.html.parsers.SAXParser
import java.util.zip.GZIPInputStream

def host = 'http://mangafox.me/manga/berserk/'
def folder = "/Volumes/Data/Manga/" + "berserk"

//def host= System.console().readLine 'Please enter the MangaFox URL: ';
//def folder = System.console().readLine 'Please enter the folder path: '

def chapterFolders = false;

def f = new File("${folder}")
f.mkdir();

def parser = new SAXParser()
def slurper = new XmlSlurper(parser)
def html = getHtml(host)
def page = slurper.parseText(html)

def chapters = page.depthFirst().findAll { it.@class == 'tips'}.collect {
    [
        link : it.@href,
        title : it.text()   
    ]
}

chapters.reverse().each { 
    def chapterName = it.title.replaceAll("[^A-Za-z0-9\\. ]", "").replaceAll("\\b(\\d)\\b", "0\$1");
    
    if(chapterFolders){
        def c = new File("${folder}/${chapterName}");
        c.mkdir();
    }
    
    def link = it.link.toString()
    def linkFolder = link.substring(0, link.lastIndexOf("/") + 1)

    html = getHtml(link)
    def chapter = slurper.parseText(html)
    def options = chapter.depthFirst().findAll { it.name() == 'OPTION' }.collect {
        [
            value : it.@value,
            path : linkFolder + it.@value + ".html"
        ]
    }

    def unique = options.unique()
        
    unique.each {
        html = getHtml(it.path)
        page = slurper.parseText(html)
        def image = page.depthFirst().find { it.@id == 'image' }.collect { it.@src }
        if(image.size() > 0){            
            String pageNumber = it.value.text().length() == 1 ? "0" + it.value : it.value;            
            if(chapterFolders){
                download("${folder}/${chapterName}/${pageNumber}.jpg", image.get(0).toString())
            } else {
                download("${folder}/${chapterName} - ${pageNumber}.jpg", image.get(0).toString())
            }
        }        
    }
}
    
private String getHtml(String host) {                            
    String html = "";
    def ins = null;
    
    while(ins == null){         
        try {
            URLConnection connection = new URL(host).openConnection();
            if (connection.getHeaderField("Content-Encoding")!=null && connection.getHeaderField("Content-Encoding").equals("gzip")){
                ins = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));            
            } else {
                ins = new BufferedReader(new InputStreamReader(connection.getInputStream()));            
            }    
        } catch(Exception e){
            println("Unable to open page: ${host}, retrying...");   
            sleep(10000);
        }
    }

      
    //End        
    String inputLine;
    while ((inputLine = ins.readLine()) != null){
        html += inputLine + "\n";
    }
    
    ins.close();
    
    return html.toString();
}

private void download(def path, String address) {
    boolean success = false; 

    while(!success){
        def file = new File(path)
                    
        if(!file.exists() || file.length() == 0){ 
            try {
                println("Downloading: ${path}")
                file.withOutputStream { out ->
                    out << new URL(address).openStream()
                }
                success = true;     
            } catch(Exception e){
                println("Error getting image: ${path}, retrying...");
                file.delete();
            }      
        } else {
            println("File exists, skipping: ${path}")
            return;
        }
        
    
    }
}
