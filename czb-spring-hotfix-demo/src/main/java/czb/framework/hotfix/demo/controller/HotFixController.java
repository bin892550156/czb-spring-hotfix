package czb.framework.hotfix.demo.controller;

import czb.framework.hotfix.core.HotFix;
import czb.framework.hotfix.demo.vo.resq.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotFixController {

    @Autowired
    HotFix hotFix;

    @PutMapping("/hotfix")
    public ApiResult hotfix(){
        //启动热修复
        hotFix.exec();
        return ApiResult.success();
    }
}
