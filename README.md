# Client de correu certificat sense tercera part de confiança

Desenvolupament d'un client basat en java amb l'objectiu de realitzar una prova de concepte del protocol de correu certificat basat en blockchain descrit a l'article:

[A Solution for Secure Certified Electronic Mail Using Blockchain as a Secure Message Board](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8654617)

Aquest client es basa en el [wallet-template](https://github.com/bitcoinj/wallet-template) de [bitcoinj](https://bitcoinj.github.io/), modificat per implementar el protocol de correu certificat.   
S'ha incorporat la part de criptografia i de l'enviament i consulta de missatges de correu necessari per el correcte funcionament del protocol.

### Descàrrega i execució

1. Primer feim un clone del repositori
<pre>
git clone --depth=1 --branch=master https://github.com/rpiza/red-wax.git
</pre>

2. Entram al directori **red-wax**
<pre>
cd red-wax
</pre>


3. Comprovam que tenim la versió de **java** adequada.
Pel desenvolument s'ha utilitzat la versió:
<pre>
java version "1.8.0_211"
Java(TM) SE Runtime Environment (build 1.8.0_211-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.211-b12, mixed mode)
</pre>
Per assegurar el correcte funcionament recomanam la versió **Java8** d'Oracle

4. Configuram adequadament el fitxer **configuration.xml** amb el valors del compte SMTP i IMAP necessaris per enviar i rebre els missatges de correu.

5. La comanda d'execució del client és:
<pre>
  #> java -cp target/*:lib/*:. com.problemeszero.redwax.Main
</pre>

6. Carregar el wallet amb bitcoins.

   Quan s'executa per primera vegada el client es realitza la sincronitació amb la xarxa blockchain de bitcoin. Per poder enviar tx al blockchain es necessari tenir bitcoins associats a l'adreça que apareix a la part superior. El wallet informa del valor de bitoins associats. Inicalment aquest valor serà **0.00**.

   6.1 Llocs web on obtenir bitcoins per a la testnet:

   - https://coinfaucet.eu/en/btc-testnet/
   - http://tpfaucet.appspot.com/
   - https://testnet-faucet.mempool.co/


7. En el següent vídeo s'expliquen les passes a seguir per executar el client i fer l'enviament d'un correu certificat en base les especificacions del protocol

[Video Tutorial]()
