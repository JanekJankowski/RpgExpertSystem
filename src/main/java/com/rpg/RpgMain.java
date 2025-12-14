package com.rpg;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.lang.reflect.Method;

public class RpgMain {

    private JFrame frame;
    private JPanel contentPanel;
    private KieSession kSession;

    public static void main(String[] args) {
        ResourceLoader.loadResources();
        SwingUtilities.invokeLater(() -> new RpgMain().start());
    }

    public void start() {
        try {
            KieServices ks = KieServices.Factory.get();
            KieContainer kContainer = ks.getKieClasspathContainer();
            kSession = kContainer.newKieSession("RpgKS");
            

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error initializing Drools: " + e.getMessage());
            return;
        }

        // GUI Setup
        frame = new JFrame("RPG Expert System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        frame.add(contentPanel);
        frame.setVisible(true);

        runLogicStep();
    }

    private void runLogicStep() {
        kSession.fireAllRules();

        Object question = null;
        java.util.List<Object> recommendations = new java.util.ArrayList<>();
        boolean hasUnansweredQuestion = false;

        for (Object fact : kSession.getObjects()) {
            if (fact.getClass().getName().contains("Question")) {
                if (!isAnswered(fact)) {
                    question = fact;
                    hasUnansweredQuestion = true;
                    break;
                }
            }
        }

        if (!hasUnansweredQuestion) {
            for (Object fact : kSession.getObjects()) {
                if (fact.getClass().getName().contains("Recommendation")) {
                    recommendations.add(fact);
                }
            }
        }

        updateUI(question, recommendations);
    }
    
    private boolean isAnswered(Object questionFact) {
        try {
            Method getId = questionFact.getClass().getMethod("getId");
            String qId = (String) getId.invoke(questionFact);
            
            for (Object fact : kSession.getObjects()) {
                if (fact.getClass().getName().contains("UserAnswer")) {
                    Method getQId = fact.getClass().getMethod("getQuestionId");
                    String ansQId = (String) getQId.invoke(fact);
                    if (ansQId.equals(qId)) return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void updateUI(Object question, java.util.List<Object> recommendations) {
        contentPanel.removeAll();

        if (question != null) {
            showQuestion(question);
        } 
        else if (recommendations != null && !recommendations.isEmpty()) {
            showRecommendations(recommendations);
        } 
        else {
            contentPanel.add(new JLabel("I don't know what else to ask and found no results."));
            
            JButton restartBtn = new JButton("Start Over");
            restartBtn.addActionListener(e -> {
                frame.dispose();
                if (kSession != null) kSession.dispose();
                start(); 
            });
            contentPanel.add(Box.createVerticalStrut(20));
            contentPanel.add(restartBtn);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showRecommendations(java.util.List<Object> recs) {
        try {
            contentPanel.add(new JLabel("Found " + recs.size() + " recommendation(s):"));
            contentPanel.add(Box.createVerticalStrut(15));

            for (Object rec : recs) {
                Method getSysKey = rec.getClass().getMethod("getSystemKey");
                
                String baseKey = (String) getSysKey.invoke(rec);
                
                String title = ResourceLoader.getString(baseKey);

                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                JLabel titleLbl = new JLabel(title);
                titleLbl.setFont(new Font("Arial", Font.BOLD, 16));
                card.add(titleLbl);
                
                contentPanel.add(card);
                contentPanel.add(Box.createVerticalStrut(10));
            }
            
            JButton restartBtn = new JButton("Start Over");
            restartBtn.addActionListener(e -> {
            	frame.dispose();
                kSession.dispose();
                start(); 
            });
            contentPanel.add(Box.createVerticalStrut(20));
            contentPanel.add(restartBtn);
            
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showQuestion(Object q) {
        try {
            Method getTextKey = q.getClass().getMethod("getTextKey");
            Method getType = q.getClass().getMethod("getType");
            Method getOptions = q.getClass().getMethod("getOptions");
            Method getOptionIds = q.getClass().getMethod("getOptionIds");
            Method getId = q.getClass().getMethod("getId");

            String text = ResourceLoader.getString((String) getTextKey.invoke(q));
            String type = (String) getType.invoke(q);
            List<String> options = (List<String>) getOptions.invoke(q);
            List<String> optIds = (List<String>) getOptionIds.invoke(q);
            String qId = (String) getId.invoke(q);

            JLabel qLabel = new JLabel(text);
            qLabel.setFont(new Font("Arial", Font.BOLD, 14));
            contentPanel.add(qLabel);
            contentPanel.add(Box.createVerticalStrut(10));

            if ("MULTI".equals(type)) {
            	List<JCheckBox> checkBoxes = new java.util.ArrayList<>();
                
                for (int i = 0; i < options.size(); i++) {
                    JCheckBox cb = new JCheckBox(ResourceLoader.getString(options.get(i)));
                    cb.setActionCommand(optIds.get(i));
                    checkBoxes.add(cb);
                    contentPanel.add(cb);
                }
                
                contentPanel.add(Box.createVerticalStrut(10));
                JButton submitBtn = new JButton("Next");
                submitBtn.addActionListener(e -> {
                    List<String> selectedIds = new java.util.ArrayList<>();
                    for (JCheckBox cb : checkBoxes) {
                        if (cb.isSelected()) {
                            selectedIds.add(cb.getActionCommand());
                        }
                    }
                    if (!selectedIds.isEmpty()) {
                        submitAnswer(qId, selectedIds);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Please select at least one option.");
                    }
                });
                contentPanel.add(submitBtn);

            } else {
                for (int i = 0; i < options.size(); i++) {
                    String labelKey = options.get(i);
                    String valId = optIds.get(i);
                    
                    JButton btn = new JButton(ResourceLoader.getString(labelKey));
                    btn.addActionListener(e -> {
                        List<String> singleSelection = new java.util.ArrayList<>();
                        singleSelection.add(valId);
                        submitAnswer(qId, singleSelection);
                    });
                    contentPanel.add(btn);
                    contentPanel.add(Box.createVerticalStrut(5));
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void submitAnswer(String questionId, List<String> selectedOptionIds) {
        try {
            String packageName = "rules";
            org.kie.api.definition.type.FactType type = kSession.getKieBase().getFactType(packageName, "UserAnswer");
            Object answer = type.newInstance();
            
            type.set(answer, "questionId", questionId);
            type.set(answer, "selectedOptions", selectedOptionIds);

            kSession.insert(answer);
            runLogicStep();

        } catch (Exception e) { e.printStackTrace(); }
    }
}