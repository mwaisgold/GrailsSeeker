package redisseeker

import grails.test.*
import redisSeeker.RedisSeeker
import redis.seek.Entry
import redis.clients.jedis.Jedis

class SeekerTests extends GrailsUnitTestCase {

  def seeker
  def index

  static INDEX_NAME = "items"

  protected void setUp() {
    super.setUp()
    seeker = new RedisSeeker(servers: ["127.0.0.1"])
    seeker.init()

    index = seeker.seek.index(INDEX_NAME)

  }

  protected void tearDown() {
    super.tearDown()

    Jedis jedis = new Jedis("127.0.0.1")
    jedis.flushAll()
  }

  void insertItem(){
    Entry entry = index.add("123")
    entry.shardBy "seller_id"
    entry.addField("seller_id", "2")
    entry.addField("status", "active")
    entry.addField("type", "normal")
    entry.addText("title", "titulin")

    entry.addOrder("start_time", System.currentTimeMillis() as Double)

    entry.save()
    println "Item saved"

  }

  void testSimpleSearch() {
    insertItem()

    def items = seeker.list {
      'index' "items"
      order "start_time"
      shard "seller_id", "2"
      field "status", "active"
    }

    items.each {
      println "******** Item: $it"
    }

    assert items.find { it == "123" } != null

  }

  void assertThrowException(Closure c, klass){
    try{
      c()
      fail()
    } catch (e){
      assertTrue e.class == klass
    }
  }

  void testSearchWithoutIndexShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        order "start_time"
        shard "seller_id", "2"
        field "status", "active"
      }
    }, IllegalStateException)
  }

  void testSearchWithoutOrderShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        'index' "items"
        shard "seller_id", "2"
        field "status", "active"
      }
    }, IllegalStateException)
  }

  void testSearchWithoutShardShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        'index' "items"
        order "start_time"
        field "status", "active"
      }
    }, IllegalStateException)
  }

  void testSearchWithoutFieldsShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        'index' "items"
        order "start_time"
        shard "seller_id", "2"
      }
    }, IllegalStateException)
  }

  void testSearchWithVariousFields(){
    insertItem()

    def items = seeker.list {
      'index' "items"
      order "start_time"
      shard "seller_id", "2"
      field "status", "active"
      field "type", "normal"
    }

    items.each {
      println "******** Item: $it"
    }

    assert items.find { it == "123" } != null
  }
}
