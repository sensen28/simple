package com.wjh.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class JsonUtil {

    /**
     * 从json字符串中解析ObjectNode
     * @param json
     * @return
     */
    public static ObjectNode getObjectNode(String json) {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode objectNode = null;
        try {
            objectNode = jsonMapper.readValue(json, ObjectNode.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return objectNode;
    }

    /**
     * 创建一个新的objectNode，用于封装json字符串
     * @return
     */
    public static ObjectNode getObjectNode(){
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.createObjectNode();
    }
}
