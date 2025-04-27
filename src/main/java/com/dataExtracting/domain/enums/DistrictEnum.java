package com.dataExtracting.domain.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DistrictEnum {
    PUDONG("浦东新区","pudong_dsj", "pudong_dsj", "Pudong_dsj@2024"),
    HUANGPU("黄浦区","huangpuqu", "huangpuqu", "Huangpuqu@2024"),
    JINGAN("静安区","jinganqu", "jinganqu", "Jinganqu@2024"),
    XUHUI("徐汇区","xuhuishidian", "xuhuishidian", "xuhuishidian@2023"),
    CHANGNING("长宁区","changningqu", "changningqu", "Changningqu@2024"),
    PUTUO("普陀区","putuoshidian", "putuoshidian", "putuoshidian@2023"),
    HONGKOU("虹口区","hongkouqu", "hongkouqu", "Hongkouqu@2024"),
    YANGPU("杨浦区","yangpuqu", "yangpuqu", "Yangpuqu@2024"),
    BAOSHAN("宝山区","baoshanshidian", "baoshanshidian", "Baoshanshidian@2023"),
    MINHANG("闵行区","minhangqu", "minhangqu", "Minhangqu@2024"),
    JIADING("嘉定区","jiadingqu", "jiadingqu", "Jiadingqu@2024"),
    JINSHAN("金山区","jinshanqu", "jinshanqu", "Jinshanqu@2024"),
    SONGJIANG("松江区","songjiangqu", "songjiangqu", "Songjiangqu@2024"),
    QINGPU("青浦区","qingpuqu", "qingpuqu", "Qingpuqu@2024"),
    FENGXIAN("奉贤区","fengxian_dsj", "fengxian_dsj", "fengxian_dsj@2023"),
    CHONGMING("崇明区","chongming_dsj", "chongming_dsj", "chongming_dsj@2023");

    private final String name;
    private final String schema;
    private final String username;
    private final String password;

    DistrictEnum(String name, String schema, String username, String password) {
        this.name = name;
        this.schema = schema;
        this.username = username;
        this.password = password;
    }

    public static DistrictEnum fromName(String name) {
        return Arrays.stream(values())
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的区名称: " + name));
    }



}