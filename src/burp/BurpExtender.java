package burp;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class BurpExtender implements IBurpExtender, ITab, IContextMenuFactory {

    private IBurpExtenderCallbacks callbacks;
    private JPanel mainPanel;
    private JTable profilesTable;
    private DefaultTableModel profilesModel;
    private JTextField profileNameField;
    private JTextArea headersTextArea;
    private JTable matchReplaceTable;
    private DefaultTableModel matchReplaceModel;
    private List<Profile> profiles = new ArrayList<>();
    private Profile currentProfile = null;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.setExtensionName("User Changer");

        SwingUtilities.invokeLater(() -> {
            mainPanel = new JPanel(new BorderLayout());
            
            profilesModel = new DefaultTableModel(new Object[]{"Profile Name"}, 0);
            profilesTable = new JTable(profilesModel);
            profilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            profilesTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = profilesTable.getSelectedRow();
                    if (selectedRow != -1) {
                        loadProfile(profiles.get(selectedRow));  
                    }
                }
            });

            JButton addProfileButton = new JButton("Save Profile");
            JButton deleteProfileButton = new JButton("Delete Profile");

            addProfileButton.addActionListener(e -> saveProfile());
            deleteProfileButton.addActionListener(e -> deleteProfile());

            JPanel profileButtonsPanel = new JPanel();
            profileButtonsPanel.setLayout(new GridLayout(2, 1, 5, 5));
            profileButtonsPanel.add(addProfileButton);
            profileButtonsPanel.add(deleteProfileButton);

            JPanel profilesPanel = new JPanel(new BorderLayout());
            profilesPanel.add(new JLabel("User Profiles:"), BorderLayout.NORTH);
            profilesPanel.add(new JScrollPane(profilesTable), BorderLayout.CENTER);
            profilesPanel.add(profileButtonsPanel, BorderLayout.SOUTH);

            mainPanel.add(profilesPanel, BorderLayout.WEST);

            
            JPanel profileDetailsPanel = new JPanel(new BorderLayout());

            
            JPanel profileNamePanel = new JPanel(new BorderLayout());
            profileNamePanel.add(new JLabel("Profile Name:"), BorderLayout.NORTH);
            profileNameField = new JTextField();
            profileNamePanel.add(profileNameField, BorderLayout.CENTER);
            profileDetailsPanel.add(profileNamePanel, BorderLayout.NORTH);

            
            JPanel headersPanel = new JPanel(new BorderLayout());
            headersPanel.add(new JLabel("Headers (Raw Format):"), BorderLayout.NORTH);
            headersTextArea = new JTextArea(10, 40);
            JScrollPane headersScrollPane = new JScrollPane(headersTextArea);
            headersPanel.add(headersScrollPane, BorderLayout.CENTER);
            profileDetailsPanel.add(headersPanel, BorderLayout.CENTER);

            
            JPanel matchReplacePanel = new JPanel(new BorderLayout());
            matchReplacePanel.add(new JLabel("Match and Replace Rules:"), BorderLayout.NORTH);
            matchReplaceModel = new DefaultTableModel(new Object[]{"Match (Regex)", "Replace"}, 0);
            matchReplaceTable = new JTable(matchReplaceModel);
            JScrollPane matchReplaceScrollPane = new JScrollPane(matchReplaceTable);
            matchReplacePanel.add(matchReplaceScrollPane, BorderLayout.CENTER);

            JButton addRuleButton = new JButton("Add Rule");
            JButton removeRuleButton = new JButton("Remove Rule");
            addRuleButton.addActionListener(e -> matchReplaceModel.addRow(new Object[]{"", ""}));
            removeRuleButton.addActionListener(e -> {
                int selectedRow = matchReplaceTable.getSelectedRow();
                if (selectedRow != -1) {
                    matchReplaceModel.removeRow(selectedRow);
                }
            });

            JPanel ruleButtonsPanel = new JPanel();
            ruleButtonsPanel.add(addRuleButton);
            ruleButtonsPanel.add(removeRuleButton);
            matchReplacePanel.add(ruleButtonsPanel, BorderLayout.SOUTH);

            profileDetailsPanel.add(matchReplacePanel, BorderLayout.SOUTH);

            mainPanel.add(profileDetailsPanel, BorderLayout.CENTER);

            callbacks.addSuiteTab(BurpExtender.this);
            callbacks.registerContextMenuFactory(BurpExtender.this);
        });
    }

    private void saveProfile() {
        String name = profileNameField.getText().trim();
        String headers = headersTextArea.getText().trim();
        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < matchReplaceModel.getRowCount(); i++) {
            String match = (String) matchReplaceModel.getValueAt(i, 0);
            String replace = (String) matchReplaceModel.getValueAt(i, 1);
            rules.add(new Rule(match, replace));
        }

        if (currentProfile == null) {
            
            Profile newProfile = new Profile(name, headers, rules);
            profiles.add(newProfile);
            profilesModel.addRow(new Object[]{newProfile.name});
        } else {
            
            currentProfile.name = name;
            currentProfile.headers = headers;
            currentProfile.rules = rules;

            int selectedRow = profilesTable.getSelectedRow();
            profilesModel.setValueAt(currentProfile.name, selectedRow, 0);
        }

        clearProfileDetails();
    }

    private void deleteProfile() {
        int selectedRow = profilesTable.getSelectedRow();
        if (selectedRow != -1) {
            profiles.remove(selectedRow);
            profilesModel.removeRow(selectedRow);
            clearProfileDetails();
        }
    }

    private void loadProfile(Profile profile) {
        
        currentProfile = profile;
        profileNameField.setText(profile.name);  
        headersTextArea.setText(profile.headers);  
        
        
        matchReplaceModel.setRowCount(0);  
        for (Rule rule : profile.rules) {
            matchReplaceModel.addRow(new Object[]{rule.match, rule.replace});  
        }
    }


    private void clearProfileDetails() {
        currentProfile = null;
        profileNameField.setText("");
        headersTextArea.setText("");
        matchReplaceModel.setRowCount(0);
    }

    @Override
    public String getTabCaption() {
        return "User Changer";
    }

    @Override
    public Component getUiComponent() {
        return mainPanel;
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> menuItems = new ArrayList<>();
        for (Profile profile : profiles) {
            JMenuItem menuItem = new JMenuItem("Apply Profile: " + profile.name);
            menuItem.addActionListener(e -> applyProfile(profile, invocation));
            menuItems.add(menuItem);
        }
        return menuItems;
    }

    private void applyProfile(Profile profile, IContextMenuInvocation invocation) {
        IHttpRequestResponse[] selectedItems = invocation.getSelectedMessages();
        if (selectedItems != null && selectedItems.length > 0) {
            for (IHttpRequestResponse item : selectedItems) {
                IRequestInfo requestInfo = callbacks.getHelpers().analyzeRequest(item);
                List<String> headers = new ArrayList<>(requestInfo.getHeaders());

                
                Map<String, String> newHeaders = parseHeaders(profile.headers);  

                for (Map.Entry<String, String> entry : newHeaders.entrySet()) {
                    boolean updated = false;
                    
                    for (int i = 0; i < headers.size(); i++) {
                        
                        if (headers.get(i).startsWith(entry.getKey() + ":")) {
                            headers.set(i, entry.getKey() + ": " + entry.getValue());
                            updated = true;
                            break;
                        }
                    }
                    
                    if (!updated) {
                        headers.add(entry.getKey() + ": " + entry.getValue());
                    }
                }

                
                byte[] request = item.getRequest();
                int bodyOffset = requestInfo.getBodyOffset();
                byte[] bodyBytes = Arrays.copyOfRange(request, bodyOffset, request.length);
                String body = callbacks.getHelpers().bytesToString(bodyBytes);

                
                for (Rule rule : profile.rules) {
                    Pattern pattern = Pattern.compile(rule.match);  
                    
                    
                    for (int i = 0; i < headers.size(); i++) {
                        Matcher matcher = pattern.matcher(headers.get(i));
                        if (matcher.find()) {
                            headers.set(i, matcher.replaceAll(rule.replace));
                        }
                    }

                    
                    Matcher bodyMatcher = pattern.matcher(body);
                    body = bodyMatcher.replaceAll(rule.replace);
                }

                
                byte[] newBody = callbacks.getHelpers().stringToBytes(body);
                byte[] newRequest = callbacks.getHelpers().buildHttpMessage(headers, newBody);
                item.setRequest(newRequest);
            }
        }
    }

    private Map<String, String> parseHeaders(String headers) {
        Map<String, String> headerMap = new HashMap<>();
        String[] lines = headers.split("\\r?\\n");
        for (String line : lines) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                headerMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        return headerMap;
    }

    
    class Profile {
        String name;
        String headers;
        List<Rule> rules;

        Profile(String name, String headers, List<Rule> rules) {
            this.name = name;
            this.headers = headers;
            this.rules = rules;
        }
    }

    class Rule {
        String match;
        String replace;

        Rule(String match, String replace) {
            this.match = match;
            this.replace = replace;
        }
    }
}
