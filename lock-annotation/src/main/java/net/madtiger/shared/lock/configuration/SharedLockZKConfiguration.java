package net.madtiger.shared.lock.configuration;

import static net.madtiger.shared.lock.SharedLockConstants.PROPERTIES_PREFIX;

import net.madtiger.shared.lock.ISharedLock;
import net.madtiger.shared.lock.ZookeeperLockClient;
import net.madtiger.shared.lock.ZookeeperLockService;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 *  zookeeper 配置
 * @author Fenghu.Shi
 * @version 1.0
 */
@Configuration
public class SharedLockZKConfiguration extends AbsSharedLockConfiguration{


  /**
   * 配置 共享锁
   * @param namespace
   * @param zookeeper zk 客户端
   * @return
   */
  @Bean
  public ISharedLock defaultSharedLock(@Value("${" + PROPERTIES_PREFIX + "zk-namespace:''}") String namespace, ZooKeeper zookeeper){
    if (StringUtils.isEmpty(namespace)) {
      return new ZookeeperLockService(new ZookeeperLockClient(zookeeper));
    }
    return new ZookeeperLockService(new ZookeeperLockClient(namespace, zookeeper));
  }

}
