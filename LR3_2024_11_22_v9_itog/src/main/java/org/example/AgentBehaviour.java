package org.example;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.example.DataModel.ListData;
import org.example.DataModel.NodeData;

import java.util.List;
import java.util.Map;

// Класс AgentBehaviour.java:
// Содержит поведение агентов, включая инициацию поиска, обработку запросов и отправку результатов.
public class AgentBehaviour {

    // InitBehaviour: Это поведение инициирует процесс поиска.
    // Оно отправляет запросы всем соседям и ожидает от них ответы.
    // После завершения поиска оно выводит кратчайший путь и его длину.
    public static class InitBehaviour extends ParallelBehaviour {
        private final ListData data = new ListData(); // Объект для хранения данных о найденных путях
        private final Map<String, Integer> neighbours; // Соседи текущего агента
        private final String findNodeName; // Имя искомого агента

        // Конструктор InitBehaviour:
        // Инициализирует поведение, устанавливая соседей и имя искомого агента.
        public InitBehaviour(Map<String, Integer> neighbours, String findNode) {
            super(ParallelBehaviour.WHEN_ANY); // Используем параллельное поведение, которое завершается при любом завершении подповедения
            this.neighbours = neighbours;
            this.findNodeName = findNode;
        }

        // Метод onStart:
        // Вызывается при старте поведения. Добавляет подповедения для отправки запросов и ожидания.
        @Override
        public void onStart() {
            // Добавляем подповедение для отправки запросов соседям
            addSubBehaviour(new InitRequestBehaviour(data, neighbours, findNodeName));
            // Добавляем подповедение, которое завершится через 2000 мс
            addSubBehaviour(new WakerBehaviour(myAgent, 2000) {
                @Override
                protected void onWake() {
                }
            });
        }

        // Метод onEnd:
        // Вызывается при завершении поведения. Выводит результаты поиска, включая кратчайший путь и его длину.
        @Override
        public int onEnd() {

            // Получаем список минимальных путей
            List<NodeData> minDataList = data.getMinDataAgreeList();

            // Выводим информацию о минимальных путях
            for (NodeData minData : minDataList) {
                System.out.println("\n\nКратчайший путь из узла " + minData.firstNodeName() + " в узел " + minData.getFindNodeName() + " это "
                        + minData.getNodeNames().toString() + ", его длина равна " + minData.getTotalLength() + ".");
            }
            return 1;
        }
    }

    // InitRequestBehaviour:
    // Это подповедение, которое отправляет запросы соседям и обрабатывает ответы с performative AGREE.
    public static class InitRequestBehaviour extends Behaviour {
        private final ListData data; // Объект для хранения данных о найденных путях
        private final Map<String, Integer> neighbours; // Соседи текущего агента
        private final String findNodeName; // Имя искомого агента
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE); // Шаблон для сообщений с performative AGREE

        // Конструктор InitRequestBehaviour:
        // Инициализирует поведение, устанавливая данные, соседей и имя искомого агента.
        public InitRequestBehaviour(ListData data, Map<String, Integer> neighbours, String findNodeName) {
            this.data = data;
            this.neighbours = neighbours;
            this.findNodeName = findNodeName;
        }

        // Метод onStart:
        // Вызывается при старте поведения. Отправляет запросы всем соседям.
        @Override
        public void onStart() {
            // Отправляем запросы всем соседям
            for (String node : neighbours.keySet()) {
                ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                m.addReceiver(new AID(node, false));
                NodeData nodeData = new NodeData(myAgent.getLocalName(), findNodeName);
                nodeData.addData(node, neighbours.get(node));
                m.setContent(NodeData.dataToString(nodeData));
                myAgent.send(m);
            }
        }

        // Метод action:
        // Вызывается для обработки сообщений. Принимает сообщения с performative AGREE и добавляет их в данные.
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                NodeData nd = NodeData.parseData(msg.getContent());
                if (msg.getPerformative() == ACLMessage.AGREE) {
                    data.addDataAgree(nd);
                }
            } else {
                block();
            }
        }

        // Метод done:
        // Возвращает false, так как это поведение не завершается.
        @Override
        public boolean done() {
            return false;
        }
    }

    // RequestBehaviour:
    // Это поведение обрабатывает запросы от других агентов.
    // Если текущий агент является искомым, он отправляет результат обратно.
    // В противном случае он передает запрос дальше своим соседям.
    public static class RequestBehaviour extends Behaviour {
        private final Map<String, Integer> neighbours; // Соседи текущего агента

        // Конструктор RequestBehaviour:
        // Инициализирует поведение, устанавливая соседей.
        public RequestBehaviour(Map<String, Integer> neighbours) {
            this.neighbours = neighbours;
        }

        // Метод action:
        // Вызывается для обработки сообщений. Принимает запросы и передает их дальше своим соседям.
        // Если текущий агент является искомым, отправляет результат обратно.
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            if (msg != null) {
                System.out.println("Узел " + this.myAgent.getLocalName() + " получил информацию от узла "
                        + msg.getSender().getLocalName() + ":" + msg.getContent());

                NodeData nodeData = NodeData.parseData(msg.getContent());

                // Если текущий агент является искомым, отправляем результат обратно
                if (nodeData.getFindNodeName().equals(myAgent.getLocalName())) {
                    myAgent.addBehaviour(new SendResultBehaviour(nodeData, ACLMessage.AGREE));
                } else {
                    // Иначе передаем запрос дальше своим соседям
                    for (String nodeName : neighbours.keySet()) {
                        NodeData nd = NodeData.parseData(msg.getContent());
                        if (nd.getNodeNames().contains(nodeName)) {
                            continue;
                        }
                        ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                        m.addReceiver(new AID(nodeName, false));
                        nd.addData(nodeName, neighbours.get(nodeName));
                        m.setContent(NodeData.dataToString(nd));
                        myAgent.send(m);
                    }
                }
            } else {
                block();
            }
        }
        // Метод done:
        // Возвращает false, так как это поведение не завершается.
        @Override
        public boolean done() {
            return false;
        }
    }

    // SendResultBehaviour:
    // Это поведение отправляет результат поиска обратно инициатору.
    public static class SendResultBehaviour extends Behaviour {
        private NodeData nodeData; // Данные о пути
        private int acl; // Performative для сообщения

        // Конструктор SendResultBehaviour:
        // поведение, устанавливая данные о пути и performative.
        public SendResultBehaviour(NodeData nodeData, int acl) {
            this.nodeData = nodeData;
            this.acl = acl;
        }

        // Метод action:
        // Вызывается для отправки сообщения. Отправляет результат обратно инициатору.
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(acl);
            msg.addReceiver(new AID(nodeData.firstNodeName(), false));
            msg.setContent(NodeData.dataToString(nodeData));
            myAgent.send(msg);
            String greenColor = "\u001B[32m"; // ANSI escape code для зеленого цвета текста
            String resetColor = "\u001B[0m"; // ANSI escape code для сброса цвета (возврат к стандартному)
            System.err.println(greenColor + "Искомый узел " + nodeData.getFindNodeName() + " найден. Отправляем узлу-инициатору "
                    + nodeData.firstNodeName() + " полученную цепочку узлов:" + msg.getContent()  + resetColor);
        }

        // Метод done:
        // Возвращает true, так как это поведение завершается после отправки сообщения.
        @Override
        public boolean done() {
            return true;
        }
    }
}