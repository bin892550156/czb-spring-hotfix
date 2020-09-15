package czb.framework.hotfix.demo.vo.resq;

public class ApiResult<T> {

    private Integer code;
    private String msg;
    private T data;

    public ApiResult(T data){
        this.data=data;
    }

    public ApiResult(){}

    public static <T> ApiResult<T> success(T data){
        ApiResult<T> apiResult = new ApiResult<>(data);
        apiResult.setCode(0);
        apiResult.setMsg("SUCCESS");
        return apiResult;
    }

    public static <T> ApiResult<T> success(){
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(0);
        apiResult.setMsg("SUCCESS");
        return apiResult;
    }

    public static <T> ApiResult<T> fail(){
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(1);
        apiResult.setMsg("FAIL");
        return apiResult;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
