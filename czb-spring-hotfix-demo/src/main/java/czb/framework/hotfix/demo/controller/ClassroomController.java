package czb.framework.hotfix.demo.controller;

import czb.framework.hotfix.demo.entity.Classroom;
import czb.framework.hotfix.demo.service.ClassroomService;
import czb.framework.hotfix.demo.vo.resq.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(value = "CLASSROOM",tags = "CLASSROOM")
@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @ApiOperation("列出教室")
    @GetMapping("/list")
    private ApiResult<List<Classroom>> list(){
        return ApiResult.success(classroomService.list());
    }
}
