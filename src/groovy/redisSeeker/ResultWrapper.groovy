package redisSeeker

import redis.seek.Result

/**
 * User: mwaisgold
 * Date: 20/12/2010
 * Time: 11:32:33
 */
class ResultWrapper implements Iterable{

  Result result

  Iterator iterator() {
    result.getIds().iterator()
  }
}
