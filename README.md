 * obican java gradle/maven projekt koji sadrzi kod od kesiranja redis i coffeince, 
    dijelim ga s user servis i course servis nije micorsevis

     * “caching-library” je običan Java/Maven (ili Gradle) projekt koji sadrži samo generički kôd
       za keširanje (Caffeine/Redis helperi, composite cache manager, event listeneri…).
     * Nije mikroservis – ne pokreće se zasebno, nema endpoint-e, nema bazu, nema Docker.
   

kreiroa sam samo libray standalone libraray i puskao ga kao lokalni
./gradlew clean build publishToMavenLocal

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

