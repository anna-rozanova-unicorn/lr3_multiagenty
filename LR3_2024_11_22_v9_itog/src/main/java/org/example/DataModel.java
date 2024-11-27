package org.example;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Класс DataModel.java:
// Содержит модели данных для хранения информации о путях и конфигурации агентов.
public class DataModel {

    // CfgClass:
    // Этот класс хранит конфигурацию агента, включая информацию о том, является ли агент инициатором,
    // имя искомого агента и список соседей с весами связей.
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "cfg")
    @Getter
    public static class CfgClass {

        @XmlElement
        private boolean start = false; // Флаг, указывающий, является ли агент инициатором

        @XmlElement
        private String findNodeName; // Имя искомого агента

        @XmlElementWrapper(name = "neighbours")
        private Map<String, Integer> neighbours = new HashMap<>(); // Соседи и веса связей
    }

    // ListData:
    // Этот класс хранит список найденных путей и предоставляет методы для добавления новых путей и получения минимальных путей.
    @Getter
    public static class ListData {
        private final List<NodeData> nodeDataAgree; // Список найденных путей

        public ListData() {
            nodeDataAgree = new ArrayList<>();
        }

        // Метод addDataAgree:
        // Добавляет данные о пути в список.
        public void addDataAgree(NodeData nodeData) {
            nodeDataAgree.add(nodeData);
        }

        // Метод getMinDataAgreeList:
        // Возвращает список минимальных путей.
        public List<NodeData> getMinDataAgreeList() {
            int minLength = 0;
            List<NodeData> minDataList = new ArrayList<>();
            for (NodeData nd : nodeDataAgree) {
                if (minDataList.isEmpty() || nd.getTotalLength() < minLength) {
                    minDataList.clear();
                    minDataList.add(nd);
                    minLength = nd.getTotalLength();
                } else if (nd.getTotalLength() == minLength) {
                    minDataList.add(nd);
                }
            }
            return minDataList;
        }

        // Метод getNodeDataAgreeSize:
        // Возвращает общее количество найденных путей.
        public int getNodeDataAgreeSize() {
            return nodeDataAgree.size();
        }
    }

    // NodeData:
    // Этот класс хранит информацию о пути, включая список имен узлов и общую длину пути.
    @Getter
    @NoArgsConstructor
    public static class NodeData {
        private List<String> nodeNames = new ArrayList<>(); // Список имен узлов в пути
        private int totalLength = 0; // Общая длина пути
        private String findNodeName; // Имя искомого агента

        // Конструктор NodeData:
        // Инициализирует данные о пути, добавляя первый узел и имя искомого агента.
        public NodeData(String firstNodeName, String findNodeName) {
            this.nodeNames.add(firstNodeName);
            this.findNodeName = findNodeName;
        }

        // Метод firstNodeName:
        // Возвращает имя первого узла в пути.
        public String firstNodeName() {
            return nodeNames.get(0);
        }

        // Метод addData:
        // Добавляет данные о следующем узле в пути и обновляет общую длину пути.
        public void addData(String nodeName, int pathLength) {
            this.nodeNames.add(nodeName);
            totalLength = totalLength + pathLength;
        }

        // Метод parseData:
        // Десериализует данные из строки.
        public static NodeData parseData(String dataString) {
            return JsonParser.parseData(dataString, NodeData.class);
        }

        // Метод dataToString:
        // Сериализует данные в строку.
        public static String dataToString(NodeData nodeData) {
            return JsonParser.dataToString(nodeData);
        }
    }

    // JsonParser:
    // Этот класс предоставляет методы для сериализации и десериализации объектов в строку и обратно.
    public static class JsonParser {
        private static org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();

        // Метод dataToString:
        // Сериализует объект в строку.
        public static <T> String dataToString(T dataClass) {
            try {
                return mapper.writeValueAsString(dataClass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Метод parseData:
        // Десериализует строку в объект.
        public static <T> T parseData(String dataString, Class<T> clazz) {
            try {
                return mapper.readValue(dataString, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}