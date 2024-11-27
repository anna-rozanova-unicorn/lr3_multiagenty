package org.example;

import jade.core.Agent;
import org.example.AgentBehaviour;
import org.example.DataModel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

// Класс NodeAgent.java:
// Основной класс агента, который запускает поведение в зависимости от конфигурации.
public class NodeAgent extends Agent {

    // Метод setup:
    // Вызывается при запуске агента. Считывает конфигурацию из XML-файла и добавляет соответствующие поведения.
    @Override
    protected void setup() {
        DataModel.CfgClass cfg;
        try {
            // Создаем контекст для десериализации XML
            JAXBContext context = JAXBContext.newInstance(DataModel.CfgClass.class);
            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
            // Десериализуем конфигурацию из XML файла
            cfg = (DataModel.CfgClass) jaxbUnmarshaller.unmarshal(
                    new File("src/main/resources/" + getLocalName() + ".xml"));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        // Если агент является инициатором, добавляем поведение для инициации поиска
        if (cfg.isStart()) {
            addBehaviour(new AgentBehaviour.InitBehaviour(cfg.getNeighbours(), cfg.getFindNodeName()));
        }

        // Добавляем поведение для обработки запросов от других агентов
        addBehaviour(new AgentBehaviour.RequestBehaviour(cfg.getNeighbours()));
    }
}