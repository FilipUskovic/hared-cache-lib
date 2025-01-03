 * obican java gradle/maven projekt koji sadrzi kod od kesiranja redis i coffeince, 
    dijelim ga s user servis i course servis nije micorsevis

     * “caching-library” je običan Java/Maven (ili Gradle) projekt koji sadrži samo generički kôd
       za keširanje (Caffeine/Redis helperi, composite cache manager, event listeneri…).
     * Nije mikroservis – ne pokreće se zasebno, nema endpoint-e, nema bazu, nema Docker.
   