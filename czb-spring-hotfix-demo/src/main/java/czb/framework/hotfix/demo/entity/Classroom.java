package czb.framework.hotfix.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("classroom")
public class Classroom {

    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
