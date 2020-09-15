package czb.framework.hotfix.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import czb.framework.hotfix.demo.entity.Classroom;

import java.util.List;

public interface ClassroomService extends IService<Classroom> {

    List<Classroom> listClassroom();
}
