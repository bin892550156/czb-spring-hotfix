package czb.framework.hotfix.core.service.impl;

import czb.framework.hotfix.core.service.BinService;
import czb.framework.hotfix.core.service.CzbService;

import java.util.LinkedList;
import java.util.List;

public class CzbServiceImpl implements CzbService {

    private BinService binService;

    @Override
    public List<String> listName() {
        binService.exec("EXEC listName() ... ");
        List<String> userList=new LinkedList<>();
        userList.add("XIAO_MING");
        userList.add("XIAO_HONG");
        userList.add("XIAO_XI");
        userList.add("Xiao_BIN");
        return userList;
    }

    public BinService getBinService() {
        return binService;
    }

    public void setBinService(BinService binService) {
        this.binService = binService;
    }
}
