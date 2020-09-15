package czb.framework.hotfix.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import czb.framework.hotfix.demo.entity.User;
import czb.framework.hotfix.demo.mapper.UserMapper;
import czb.framework.hotfix.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> list(){
        List<User> users = userMapper.listUser();
        return users;
    }

    @Override
    public User getUser(User user) {
        return userMapper.getUser(user);
    }

    @Override
    public String getUserName(Long id) {
        return userMapper.getUserName(id);
    }

    @Override
    public List<User> listUseMybatisPlus(String name){
        List<User> users = list(new QueryWrapper<User>().like("name", name));
        return users;
    }

    @Override
    public void insertUser(User user){
        userMapper.insertUser(user);
    }
}
