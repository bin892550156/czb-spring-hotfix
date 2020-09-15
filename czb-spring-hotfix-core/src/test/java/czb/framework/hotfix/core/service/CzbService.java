package czb.framework.hotfix.core.service;

import java.util.List;

public interface CzbService {

    List<String> listName();

    void setBinService(BinService binService);

    BinService getBinService();
}
