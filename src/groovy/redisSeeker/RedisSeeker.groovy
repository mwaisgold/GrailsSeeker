package redisSeeker

import redis.seek.Seek
import redis.clients.jedis.JedisShardInfo
import org.apache.commons.pool.impl.GenericObjectPool.Config;

/**
 * User: mwaisgold
 * Date: 20/12/2010
 * Time: 11:09:52
 */
class RedisSeeker {

    def servers
    def maxActiveConnections = 5
    def minIdleConnections = 5
    def port = 6379

    Seek seek
    def indexes = [:]

    def init() {
        if (!servers) {
            throw new IllegalStateException("No servers for redis seeker")
        }

        def shards = servers.collect { new JedisShardInfo(it, port) }
        Config config = new Config()
        config.testOnBorrow = true
        config.testOnReturn = true
        config.maxActive = maxActiveConnections
        config.minIdle = minIdleConnections

        Seek.configure config, shards

        this.seek = new Seek()
    }


    def list(Closure c) {
        def intSeeker = new SeekerAdapter(seek: seek)
        c.delegate = intSeeker
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        intSeeker.search()
    }
    
    def info(Closure c) {
	def intSeeker = new SeekerAdapter(seek: seek)
	c.delegate = intSeeker
	c.resolveStrategy = Closure.DELEGATE_FIRST
	c()
	
	intSeeker.info()
    }
    
    def index(name) {
        indexes."$name" ?: seek.index(name)        
    }

    def save(index, stopWords, Closure c) {
        def saver = new SeekerAdapter(seek: seek)
        c.delegate = saver
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        saver.save(index, stopWords)

    }

}
