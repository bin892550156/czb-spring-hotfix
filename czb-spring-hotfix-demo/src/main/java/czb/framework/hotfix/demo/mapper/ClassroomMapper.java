package czb.framework.hotfix.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import czb.framework.hotfix.demo.entity.Classroom;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassroomMapper extends BaseMapper<Classroom> {

    @Select("select id,name from classroom")
    List<Classroom> listClassroom();
}
