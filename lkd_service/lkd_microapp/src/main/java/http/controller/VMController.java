package http.controller;

import com.lkd.feignService.VMService;
import com.lkd.viewmodel.SkuViewModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/vm")
public class VMController {
    @Autowired
    private VMService vmService;

    /**
     * 获取售货机商品列表(扫描售货机获取商品list)
     *
     * @param innerCode
     * @return
     */
    @GetMapping("/skuList/{innerCode}")
    public List<SkuViewModel> getSkuListByInnercode(@PathVariable String innerCode) {
        return vmService.getAllSkuByInnerCode(innerCode);
    }
}
