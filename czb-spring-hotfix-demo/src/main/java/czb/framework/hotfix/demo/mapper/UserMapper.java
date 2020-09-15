package czb.framework.hotfix.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import czb.framework.hotfix.demo.entity.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMapper extends BaseMapper<User> {

    @Select({" select id, name,age from user"})
    @ResultMap("userResultMap")
    List<User> listUser();

    User getUser(@Param("user") User user);

    @Select({" select  name from user where id = #{id}"})
    String getUserName(Long id);

    @SelectKey(statement = "SELECT MAX(id)+1 FROM user ", keyProperty = "id", before = true, resultType = Long.class)
    @Insert("INSERT INTO user (id, name) VALUES(#{id}, #{name})")
    void insertUser(User user);
}
