package czb.framework.hotfix.demo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import czb.framework.hotfix.demo.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    List<User> list();

    User getUser(User user);

    String getUserName(Long id);

    List<User> listUseMybatisPlus(String name);

    void insertUser(User user);
}
