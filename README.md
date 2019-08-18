# Client de correu certificat sense tercera part de confiança

Desenvolupament d'un client basat en java amb l'objectiu de realitzar una prova de concepte del protocol de correu certificat basat en blockchain descrit a l'article:

[A Solution for Secure Certified Electronic Mail Using Blockchain as a Secure Message Board](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8654617)



### Obtenir bitcoins per a la testnet
- https://coinfaucet.eu/en/btc-testnet/
- http://tpfaucet.appspot.com/
- https://testnet-faucet.mempool.co/

### Comanda d'execució

La versió de java utilitzada per realitzar el desenvolupament és:

<pre>
java version "1.8.0_211"
Java(TM) SE Runtime Environment (build 1.8.0_211-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.211-b12, mixed mode)
</pre>

Per assegurar el correcte funcionament recomanam la versió java8 d'Oracle

La comanda d'execució és:

<pre>
  #> java -cp target/*:lib/*:. com.problemeszero.redwax.Main
</pre>
