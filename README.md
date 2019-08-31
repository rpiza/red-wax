# Client de correu certificat sense tercera part de confiança

Desenvolupament d'un client basat en java amb l'objectiu de realitzar una prova de concepte del protocol de correu certificat basat en blockchain descrit a l'article:

[A Solution for Secure Certified Electronic Mail Using Blockchain as a Secure Message Board](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8654617)

Aquest client es basa en el [wallet-template](https://github.com/bitcoinj/wallet-template) de [bitcoinj](https://bitcoinj.github.io/), modificat per implementar el protocol de correu certificat.   
S'ha incorporat la part de criptografia i de l'enviament i consulta de missatges de correu necessari per el correcte funcionament del protocol.

Aquesta branca utilitza la version de bitcoinj-0.15.3 que permet l'ús d'adreces SegWit. Per la seva correcta execució es necessari empra Java 11.

#### Llibreries openjfx-11

Java 11 té l'inconvenient que ja no incorpora la llibreries necessàries per executar aplicacions JavaFX.

##### Llibreries openjfx-11 en temps d'execució

Per realitzar la instal·lació d'aquestes llibreries hem de seguir les següents passes:
* descarregar el fitxer SDK corresponent al sistema operatiu desitjat del lloc web [https://gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)
* descomprimir el fitxer descarregat
* recordar la ubicació, ja que serà necessari introduir-la en la comanda del punt 5, com a paràmetre de **--module-path**

A més d'instal·lar les llibreries necessàries en temps d'execució també és necessari emprar les llibreries adequades en temps de compilació.

##### Llibreries openjfx-11 en temps de compilació:

En aquest cas, el fitxer build.gradle conté les instruccions adequades per detectar el sistema operatiu de l'ordinador i compilar amb les llibreries adequades al sistema operatiu.

Segment del fitxer build.gradle:
```
...

def currentOS = org.gradle.internal.os.OperatingSystem.current()
def platform
if (currentOS.isWindows()) {
    platform = 'win'
} else if (currentOS.isLinux()) {
    platform = 'linux'
} else if (currentOS.isMacOsX()) {
    platform = 'mac'
}

...

dependencies {

...

    compile "org.openjfx:javafx-base:11:${platform}"
    compile "org.openjfx:javafx-graphics:11:${platform}"
    compile "org.openjfx:javafx-controls:11:${platform}"
    compile "org.openjfx:javafx-fxml:11:${platform}"

}
....

```

##### Generació del Jar

El fitxer build.gradle incorpora la tasca **fatJar**, que genera un jar amb la gran part de les llibreries necessàries per a l'execució de l'aplicació.

Segment del fitxer build.gradle:
```

...

//create a single Jar with all dependencies
task fatJar(type: Jar) {
	manifest {
        attributes ('Implementation-Title': 'Red-Wax Jar',
        	'Implementation-Version': version,
        	'Main-Class': 'com.problemeszero.redwax.Main')
    }
    baseName = 'com.problemeszero.redwax-' + "${platform}"
    from { configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } }{

}
    with jar
}

...

```

Per facilitar l'execució, i evitar d'haver de generar el jar pel sistema operatiu desitjat, el directori build/libs conté un jar per cada sistema operatiu. A la comanda del punt 5, en la definició del classpath, introduïu el jar adequat al vostre sistema operatiu.

### Descàrrega i execució

1. Primer feim un clone del repositori
<pre>
#> git clone --depth=1 --branch=redwax-0.15.3 https://github.com/rpiza/red-wax.git
</pre>

2. Entram al directori **red-wax**
<pre>
#> cd red-wax
</pre>

3. Comprovam que tenim la versió de **java** adequada. Pel desenvolupament s'ha utilitzat la versió:
<pre>
#> java -version
   openjdk version "11.0.4" 2019-07-16
   OpenJDK Runtime Environment 18.9 (build 11.0.4+11)
   OpenJDK 64-Bit Server VM 18.9 (build 11.0.4+11, mixed mode, sharing)
</pre>
Per assegurar el correcte funcionament recomanam la versió **Java11** d'Oracle

4. Configuram adequadament el fitxer **configuration.xml** amb el valors del compte SMTP i IMAP necessaris per enviar i rebre els missatges de correu.

5. Execució del client amb la comanda:
<pre>
 #> java --module-path /opt/javafx-11.0.2/lib --add-modules javafx.fxml,javafx.controls -cp build/libs/com.problemeszero.redwax-linux-0.15.3.jar:lib/*:. com.problemeszero.redwax.Main
</pre>

6. Carregar el wallet amb bitcoins.

   Quan s'executa per primera vegada el client es realitza la sincronitació amb la xarxa blockchain de bitcoin. Per poder enviar tx al blockchain es necessari tenir bitcoins associats a l'adreça que apareix a la part superior. El wallet informa del valor de bitcoins associats. Inicalment aquest valor serà **0.00**.

   6.1 Llocs web on obtenir bitcoins per a la testnet:

   - https://coinfaucet.eu/en/btc-testnet/
   - http://tpfaucet.appspot.com/
   - https://testnet-faucet.mempool.co/


7. En el següent vídeo s'expliquen les passes a seguir per executar el client i fer l'enviament d'un correu certificat en base les especificacions del protocol:

   [Video Tutorial](http://htmlpreview.github.io/?https://github.com/rpiza/red-wax/blob/tutorial/media/tutorial.html)

   Per simplicitat en el tutorial assumim que n'Alice i en Bob tenen la mateixa adreça de correu, així només utilitzam una instància del client.

   Per simular un cas real, s'ha de duplicar el directori **red-wax** descarregat i modificar el **configuration.xml** de cada directori amb les dades de n'Alice i de'n Bob respectivament. Finalment executar una instància del client de cada directori.   





### Problemes detectats

1. Modificació dels missatges de correu per part del servidors SMTP

   Hem detectat que el servidor de smtp de gmail realitza modificacions al contingut dels missatges correus. Modifica els finals de línia i les tabulacions, així com també el Content-Type de les diferents parts del missatge.

   Els missatges semblen idèntics a l'ull, però a l'hora de validar les signatures la modificació de aquests continguts afecta la validació de la signatura del missatge, donant-la com invàlida. El client permet a l'usuari continuar malgrat la signatura sigui invàlida.

   Per evitar els problemes amb gmail s'ha utilitzat el servidor smtp d'OVH, i encara que aquest també modifica els finals de línia del body de missatge de correu per programació es corregit a l'origen, i el funcionament és correcte.
