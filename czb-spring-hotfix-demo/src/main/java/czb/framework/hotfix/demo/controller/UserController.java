package czb.framework.hotfix.demo.controller;

import czb.framework.hotfix.demo.entity.User;
import czb.framework.hotfix.demo.service.UserService;
import czb.framework.hotfix.demo.vo.resq.ApiResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "USER",tags = "USER")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    @ApiOperation("列出用户")
    public ApiResult<List<User>> listUser(){
        return ApiResult.success(userService.list());
    }

    @GetMapping("/listUseMybatisPlus")
    @ApiOperation("列出用户")
    public ApiResult<List<User>> listUseMybatisPlus(@RequestParam(value = "name",required = false) String name){
        return ApiResult.success(userService.listUseMybatisPlus(name));
    }

    @GetMapping("/get")
    @ApiOperation("获取用户")
    public ApiResult<User> getUser(@RequestParam(value = "id") Long id,
                                   @RequestParam(value = "name",required = false)String name,
                                   @RequestParam(value = "age",required = false)Integer age){
        return ApiResult.success(userService.getUser(new User(id,name,age)));
    }

    @GetMapping("/username/{id}")
    @ApiOperation("获取用户名")
    public ApiResult<String> getUserName(@PathVariable("id") Long id){
        return ApiResult.success(userService.getUserName(id));
    }

    @GetMapping("/test/hotfix/{id}")
    @ApiOperation("测试HOTFIX")
    public ApiResult<String> testHofix(@PathVariable("id") Long id){
        return ApiResult.success(String.valueOf(id));
    }

    @PostMapping("/insert")
    @ApiOperation("测试HOTFIX")
    public ApiResult insertUser(@RequestParam(value = "id") Long id,
                                @RequestParam(value = "name",required = false)String name){
        userService.insertUser(new User(null,name,null));
        return ApiResult.success();
    }
}
