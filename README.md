 * obican java gradle/maven projekt koji sadrzi kod od kesiranja redis i coffeince, 
    dijelim ga s user servis i course servis nije micorsevis

     * “caching-library” je običan Java/Maven (ili Gradle) projekt koji sadrži samo generički kôd
       za keširanje (Caffeine/Redis helperi, composite cache manager, event listeneri…).
     * Nije mikroservis – ne pokreće se zasebno, nema endpoint-e, nema bazu, nema Docker.
   

kreiroa sam samo libray standalone libraray i puskao ga kao lokalni
./gradlew clean build publishToMavenLocal
imam u grale u api umjesto implementations 
 implementations zanci da -> ovisnost ostaje skrivena od projekata koje koriste implementaciju 
 api znaci da -> visnost postaje viljdiva projektima koji koriste 

 - našem slucaju trebamo api jer ce nasi user-service i courses-service treba pristup klasam 
 - treaba pristup caffience i redis klasam za configruaciju
 - treba pristup microMeter klasma i registirjema 
  
./gradlew publishToMavenLocal


  Kako funkcioniraju klase
   1. Inicijalizacija MonitoredCacheManager:
    -> U sprigu konfi korsitmo MonitoredCacheManager umjesto standardnog CacheManager-a
   2. Zamotavam cache (Cache wrapper) 
    -> Svaki cache dohvacnm putem MonitoredCacheManager automatski obavlja MonitoredCache klasu
   3. Pracenje configuracije:
    -> svaki putaka kad se get put ili eviction pozove nad kesom automatski se biljezi putem metrcisServic-a

   to nam momogucuje
   * centralizirano upravljanje metrikama -> pracenje podataka i promasaja dosljedno krzo cijelu app
   * laka integraijca s prometheusom/grafanom 
   * Smanjejne dupliciranja koda - svi "cache" pozivi koriste istu metodu monitorCacheOperation za pracenje poziva
   * lako dodati novu logiku samo dodati novo u CacheMetricsService

    Caffeine cache
  1. Konfiguracije cache.a CaffeineCacheConfigurationHelper stvara CustomCaffeineCacheManager
  2. Praćenje Događaja -> CustomCaffeineCacheManager korsiti removalListener za pracenje eviction događaja i šalje ih cacheEventListeneru
  3. Metrike -> MeterRegistry se brine za pracenje metrika i biljezi hit miss viction
  4. Strategije -> Enum CacheStrategy definira razlicite strategije 

  prednosti:
  * centralizirano upravljanje cache metrika -> sve cache operacije (get, put, eviciton) se prate kroz Micrometer
  * Modularnost i proširivst -> lako dodati nove CcaesStrategije u enum 
  * Event lISTENER -> pracenje i logiranje evicitona pruza nam dodadni uvid u cache performance


  Te imamo i CacheHealtIndiciator:
   -> koji autimatski provjerava zdravlje za sve vrsta keša kojiega korsitimo pocomocu springboot acuator-a
  
 
 Redis cahce -> kada applikacija zatrazi spremanje i dohvat podatka iz redis-a:
 1. RedisTemplae -> RedisConfiguration se korsiit za direktno čitanje i pisanje u Redis-u
 2. RedisCacheManager -> "RedisCacheConfigurationHelper"  se korsiti za upravljanje keširanjem podatka na abstraktom sloju

 Prednosti:

 * RedisCacheManager - omogucava cache na najvisem nivou app
 * RedisTemplate omogucava direktan pristup redis-u za posebne iperacije koje cacahe manager ne moze obraditi
 * Modularsnot razdvajanje redis configuracije i naprednih strategija, promjene se mogu izvrsiti bez mjenjanja temeljne confgi
 * performance i fleksibilnost

 vizualni prikza bi izgledao ovako nekaok 

        User Request
    |
    v
 [CompositeCacheManager]
|
|----> [Caffeine Cache] (L1 - brza provjera)
|            |      
|            v      
|       Cache Hit? (DA - vraća podatak)
|            |      
|           NE      
|            |
|            v
|----> [Redis Cache] (L2 - provjera)
|            |      
|            v      
|       Cache Hit? (DA - vraća podatak i stavlja u Caffeine)
|            |      
|           NE      
|            |
|            v
|----> [DB / Service Call] (Spremi u Redis i Caffeine)
            