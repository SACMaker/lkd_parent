package com.lkd.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.google.common.collect.Lists;
import com.lkd.entity.AbstractEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Excel解析批量存储(通用ExcelDataListener)
 *
 * @param <T>
 */
@Slf4j
public class ExcelDataListener<T extends AbstractEntity,E> extends AnalysisEventListener<E> {
    //持久化的实体对象类型
    private Class<T> clazz;
    /**
     * 每隔500条存储数据库
     */
    private static final int BATCH_COUNT = 500;
    //存储的具体操作-函数式接口
    private Function<Collection<T>,Boolean> saveFunc;
    //批量存储的数据
    private List<T> list = Lists.newArrayList();
    @Override
    public void invoke(E e, AnalysisContext analysisContext) {
        try {
           T t = clazz.getDeclaredConstructor(null).newInstance();//得到持久化的实体对象类型
            //e是excel的映射对象
            BeanUtils.copyProperties(e,t);
            list.add(t);
            //大于等于500就可以存进去了
            if (list.size() >= BATCH_COUNT) {
                this.saveFunc.apply(list);
                // 存储完成清理 list
                list.clear();
            }
        } catch (Exception ex) {
            log.error("create new instance error",ex);
        }


    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //小于500执行的存储操作
        this.saveFunc.apply(list);
    }

    /**通过ExcelDataListener的构造器重载把Function<Collection<T>, Boolean> saveFunc:持久化的方法实现
     *                                 Class<T> clazz:持久化的实体对象类型
     *参数化用来构造通用的数据导入方法
     * @param saveFunc
     * @param clazz
     */
    public ExcelDataListener(Function<Collection<T>, Boolean> saveFunc/*持久化的方法实现*/, Class<T> clazz/*持久化的实体对象类型*/) {
        this.saveFunc = saveFunc;
        this.clazz = clazz;
    }
}
/*
@Component
public class ExcelDataListener<E> extends AnalysisEventListener<E> {
    private List<SkuEntity> list = Lists.newArrayList();

    @Autowired
    private SkuService skuService;

    private static final int BATCH_COUNT = 500;//每批存储条数

    */
/**
 * 提取数据
 *
 * @param e
 * @param analysisContext
 *//*

    @Override
    public void invoke(E e, AnalysisContext analysisContext) {
        SkuEntity sku = new SkuEntity();
        BeanUtils.copyProperties(e, sku);
        list.add(sku);
        if (list.size() >= BATCH_COUNT) {
            //等于500条就可以存进db,并清空list
            doAfterAllAnalysed(null);
        }
    }


    */
/**
 * 读取完成
 *
 * @param analysisContext
 *//*

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

        skuService.saveBatch(list);
        list.clear();
    }
}*/
