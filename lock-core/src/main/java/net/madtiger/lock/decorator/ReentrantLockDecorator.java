package net.madtiger.lock.decorator;

import static net.madtiger.lock.AbsSharedLockDecorator.ORDER_DEFAULT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.madtiger.lock.AbsSharedLockDecorator;
import net.madtiger.lock.CompositeSharedLock;
import net.madtiger.lock.SharedLockStatus;
import net.madtiger.lock.exception.TimeoutSharedLockException;
import org.slf4j.Logger;
import org.springframework.core.annotation.Order;

/**
 * 可重入锁
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
@Slf4j
@Order(ORDER_DEFAULT)
public class ReentrantLockDecorator extends AbsSharedLockDecorator {

  private static final ThreadLocal<Map<String, ReentrantLockDecorator>> THREAD_LOCKS = new ThreadLocal<Map<String, ReentrantLockDecorator>>(){
    @Override
    protected Map<String, ReentrantLockDecorator> initialValue() {
      return new HashMap<>(8);
    }
  };

  private SharedLockStatus currentStatus;

  /**
   * 获取到锁的时间
   */
  private long getLockTime;

  /**
   * 构造一个 包装器实例
   *
   * @param delegate 实际执行者
   */
  public ReentrantLockDecorator(CompositeSharedLock delegate) {
    super(delegate);
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    // 如果当前线程存在，则直接返回
    try {
      if (exisitsByThread()) {
        debugMessage("重用锁成功");
        return true;
      }else {
        // 获取锁
        if (delegate.tryLock(time, unit)) {
          debugMessage("获取新锁成功");
          setToThread();
          return true;
        }
      }
      // 如果之前的线程锁超时，则直接设置为超时
    } catch (TimeoutSharedLockException e) {
      currentStatus = SharedLockStatus.CANCEL;
      delegate.interrupted();
      debugMessage("锁获取失败且取消成功");
    }
    return false;
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  public void unlock() {

    // 如果当前清理成功
    if (clearFromThread()) {
      delegate.unlock();
      debugMessage("彻底释放锁成功");
    }else {
      debugMessage("释放锁成功");
    }
  }


  /**
   * 检查当前线程是否已经获取了锁
   * @return 是否存在
   */
  boolean exisitsByThread() throws TimeoutSharedLockException {
    // 获取锁
    ReentrantLockDecorator lock = THREAD_LOCKS.get().get(getKey());
    if (lock == null){
      return false;
    }
    // 检查锁的时间
    if (System.currentTimeMillis() - lock.getLockTime > TimeUnit.MILLISECONDS.toMillis(lock.getLockSeconds())) {
      // 移除当期那锁
      throw new TimeoutSharedLockException(getKey());
    }
    return true;
  }

  @Override
  public SharedLockStatus getStatus() {
    return currentStatus == null ? super.getStatus() : currentStatus;
  }

  /**
   * 把当前锁设置到当前线程中
   */
  void setToThread(){
    getLockTime = System.currentTimeMillis();
    THREAD_LOCKS.get().put(getKey(), this);
  }

  /**
   * 从当前线程清空
   */
  boolean clearFromThread(){
    // 获取锁
    ReentrantLockDecorator lock = THREAD_LOCKS.get().get(getKey());
    if (lock != null && lock == this){
      THREAD_LOCKS.get().remove(getKey());
      return true;
    }
    return false;
  }
}
