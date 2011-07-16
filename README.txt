

1st) edit pom.xml

-- ${play.framework.jar}


2nd) change copy.sh paths


3nd) build:

   mvn clean package assembly:assembly && sh copy.sh   

4th) configure AS:

- standalone.xml

  -- load play extension 
    <extension module="org.jboss.extension.play"/>
    
  -- setup samples folder
    <paths>
      <path name="play.framework" path="/home/emuckenh/Downloads/play-1.2.2/samples-and-tests" />
    </paths>
    
  -- add subsystem
    <subsystem xmlns="urn:org.playframework.subsystem:1.0">
        <framework-path path="/home/emuckenh/Downloads/play-1.2.2" />
    </subsystem>

    
5th) run: perl deploy.pl

  required perl JSON (sudo yum install perl-JSON)
  
  $filename represents relative folder to sample and tests
  
  