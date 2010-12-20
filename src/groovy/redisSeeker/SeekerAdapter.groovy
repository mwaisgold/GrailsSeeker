package redisSeeker

import redis.seek.Seek
import redis.seek.ShardField
import redis.seek.Search

/**
 * User: mwaisgold
 * Date: 20/12/2010
 * Time: 11:11:11
 */
class SeekerAdapter {
  Seek seek

  def fields = [:]
  def texts = [:]
  def shardFields = []
  def index
  def order
  def tags = []

  def cache = 0;
  def from = 0
  def to = 49
  def asc = true

  def field(name, String... values){
    fields."$name" = values
  }

  def shard(name, value){
    shardFields << new ShardField(name, value)
  }

  def index(name){
    this.index = name
  }

  def order(name, asc = "asc"){
    this.order = name
    this.asc = asc == "asc"
  }

  def cache(time){
    this.cache = time
  }

  def from(from){
    this.from = from
  }

  def to(to){
    this.to = to
  }

  def tag(tagName){
    tags << tagName
  }

  def text(field, value){
    texts."$field" = value
  }

  private validate(field, message){
    if (!field){
      throw new IllegalStateException(message)
    }
  }

  def search(){
    validate(index,"You must provide an index to search in")
    validate(order,"You must provide an order to search")
    validate(shardFields,"You must provide a shard field and it's value")
    validate(fields,"You must send fields to search with")
    validate(to >= from, "To must be greater than from")

    Search search = seek.search(index, order, (ShardField[])shardFields.toArray())
    fields.each { k,v ->
      search.field k, v
    }

    if (tags)
      search.tag((String[])tags.toArray())

    texts.each { k,v ->
        search.text k,v
    }

    //println "RANGE: [$from - $to]"
    new ResultWrapper(result: search.run(cache, from, to, asc? Search.Order.ASC : Search.Order.DESC))
  }

}
