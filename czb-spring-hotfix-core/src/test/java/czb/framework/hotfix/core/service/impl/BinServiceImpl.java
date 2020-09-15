package czb.framework.hotfix.core.service.impl;

import czb.framework.hotfix.core.service.BinService;

public class BinServiceImpl implements BinService {

    @Override
    public void exec(String command) {
        System.out.println(" BinService exec : "+command);
    }

}
