rm -rf apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/
mkdir -p apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/searchEngine
mkdir apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/src
javac -cp "lib/*" src/*.java SearchEngine.java
cp database.* apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/searchEngine
cp stopwords.txt apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/searchEngine
cp -r src/*.class apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/src
cp SearchEngine*.class apache-tomcat-11.0.0-M19/webapps/comp4321/WEB-INF/classes/searchEngine


