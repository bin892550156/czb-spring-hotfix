package czb.framework.hotfix.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import czb.framework.hotfix.demo.entity.Classroom;
import czb.framework.hotfix.demo.mapper.ClassroomMapper;
import czb.framework.hotfix.demo.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomServiceImpl extends ServiceImpl<ClassroomMapper, Classroom> implements ClassroomService {

    @Autowired
    private ClassroomMapper classroomMapper;


    @Override
    public List<Classroom> listClassroom() {
        return classroomMapper.listClassroom();
    }
}
