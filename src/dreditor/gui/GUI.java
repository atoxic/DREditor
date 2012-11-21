package dreditor.gui;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import dreditor.*;
import dreditor.font.*;
import org.json.JSONException;

/**
 *
 * @author /a/nonymous scanlations
 */
public class GUI extends javax.swing.JFrame
{
    private DefaultTableModel umdimageModel, umdimage2Model;
    private TableRowSorter<DefaultTableModel> umdimageSorter, umdimage2Sorter;
    private Config config;
    private boolean validWorkspace;
    
    // Preview images
    private BufferedImage textPVImage, buildPVImage;
    // For executing image updates in a queue
    private Executor exec;
    
    /**
     * Creates new form GUI
     */
    public GUI()
    {
        config = new Config();
        exec = Executors.newFixedThreadPool(1);
        preInitUnpackFiles();
        
        initComponents();
        
        checkWorkspace();
        postInitTextPV();
        postInitUnpackFiles();
        postInitSettings();
        
        PrefsUtils.registerWindow("main", this, false);
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // Set up file choosers
                workspaceChooser.setSelectedFile(DREditor.workspace);
                workspaceField.setText(DREditor.workspace.getAbsolutePath());
                
                String iso = PrefsUtils.PREFS.get("iso", null);
                if(iso != null)
                {
                    isoChooser.setSelectedFile(new File(iso));
                    isoField.setText(iso);
                }
            }
        });
    }
    
    private void postInitTextPV()
    {
        textPVArea.getDocument().addDocumentListener(  
            new DocumentListener()  
            {
                @Override
                public void changedUpdate(DocumentEvent e)  
                {
                    updateTextPV();
                }
                @Override
                public void insertUpdate(DocumentEvent e)  
                {
                    updateTextPV();
                }
                @Override
                public void removeUpdate(DocumentEvent e)  
                {
                    updateTextPV();
                }
            }
        );
        textPVDialog.pack();
        PrefsUtils.registerWindow("textPV", textPVDialog, false);
    }
    
    private void preInitUnpackFiles()
    {
        umdimageModel = new DefaultTableModel()
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return(column == 0);
            }
        };
        setupModel(umdimageModel);
        umdimageSorter = new TableRowSorter<>(umdimageModel);
        
        umdimage2Model = new DefaultTableModel()
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return(column == 0);
            }
        };
        setupModel(umdimage2Model);
        umdimage2Sorter = new TableRowSorter<>(umdimage2Model);
    }
    
    private void postInitUnpackFiles()
    {
        unpackFileSearch.getDocument().addDocumentListener(  
            new DocumentListener()  
            {
                @Override
                public void changedUpdate(DocumentEvent e)  
                {
                    newFilter();  
                }
                @Override
                public void insertUpdate(DocumentEvent e)  
                {
                    newFilter();  
                }
                @Override
                public void removeUpdate(DocumentEvent e)  
                {
                    newFilter();  
                }
            }
        );
        
        umdimageFileTable.getColumnModel().getColumn(0).setMaxWidth(50);
        umdimageFileTable.getColumnModel().getColumn(1).setMaxWidth(50);
        umdimage2FileTable.getColumnModel().getColumn(0).setMaxWidth(50);
        umdimage2FileTable.getColumnModel().getColumn(1).setMaxWidth(50);
        
        unpackFilesDialog.pack();
        PrefsUtils.registerWindow("files", unpackFilesDialog, false);
    }
    
    private void postInitSettings()
    {
        DocumentListener dl = new DocumentListener()  
        {
            @Override
            public void changedUpdate(DocumentEvent e)  
            {
                updateBuildPV();
            }
            @Override
            public void insertUpdate(DocumentEvent e)  
            {
                updateBuildPV();
            }
            @Override
            public void removeUpdate(DocumentEvent e)  
            {
                updateBuildPV();
            }
        };
        
        buildVersion.getDocument().addDocumentListener(dl);
        buildAuthors.getDocument().addDocumentListener(dl);
        buildComments.getDocument().addDocumentListener(dl);
        settingsDialog.pack();
        PrefsUtils.registerWindow("settings", settingsDialog, false);
        
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // Set up settings
                loadSettings();
            }
        });
    }
    
    private static boolean checkISOFile(File f)
    {
        return(f != null && f.exists() && f.isFile());
    }
    
    private File getISOFile()
    {
        if(!checkISOFile(isoChooser.getSelectedFile()))
            isoChooser.showOpenDialog(this);
        return(checkISOFile(isoChooser.getSelectedFile()) ? isoChooser.getSelectedFile() : null);
    }
    
    private static void setupModel(DefaultTableModel model)
    {
        model.addColumn("Unpack?");
        model.addColumn("Index");
        model.addColumn("File");
    }
    
    private static void setModel(DefaultTableModel model, UmdPAKFile umdFile)
    {
        try
        {
            java.util.List<UmdFileInfo> list = BinFactory.parseTOC(umdFile);
            while(model.getRowCount() > 0)
                model.removeRow(0);
            for(UmdFileInfo file : list)
            {
                Object[] row = new Object[]{false, file.index, file.file};
                model.addRow(row);
            }
        }
        catch(IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
    }
    
    private void newFilter()  
    {
        final String[] terms = unpackFileSearch.getText().split("\\s");
        RowFilter<DefaultTableModel, Object> rf = new RowFilter<DefaultTableModel, Object>()
        {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry)
            {
                String file = (String)entry.getValue(2);
                for(String term : terms)
                    if(!file.contains(term))
                        return(false);
                return(true);
            }
        };
        umdimageSorter.setRowFilter(rf);
        umdimage2Sorter.setRowFilter(rf);
    }

    public void loadSettings()
    {
        umdimage.setSelected(config.PACK_UMDIMAGE);
        umdimage2.setSelected(config.PACK_UMDIMAGE2);
        langCombo.setSelectedIndex(config.HOME_SCREEN_LANG);
        confirmGroup.setSelected(confirmX.getModel(), config.BUTTON_ORDER_SWITCHED);
        buildScreen.setSelected(config.BUILD_SCREEN);
        buildVersion.setText(config.VERSION);
        buildAuthors.setText(config.AUTHOR);
        buildComments.setText(config.COMMENT);
    }
    
    public boolean saveSettings()
    {
        if(!umdimage.isSelected() && !umdimage2.isSelected())
        {
            setStatus(GUIUtils.BUNDLE.getString("GUI.pack.chooseOne.text"));
            return(false);
        }
        config.PACK_UMDIMAGE = umdimage.isSelected();
        config.PACK_UMDIMAGE2 = umdimage2.isSelected();
        config.HOME_SCREEN_LANG = langCombo.getSelectedIndex();
        config.BUTTON_ORDER_SWITCHED = confirmX.isSelected();
        config.BUILD_SCREEN = buildScreen.isSelected();
        config.VERSION = buildVersion.getText();
        config.AUTHOR = buildAuthors.getText();
        config.COMMENT = buildComments.getText();
        return(true);
    }
    
    public void setStatus(final String text)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                statusBar.setText(text);
            }
        });
    }
    
    public void setProgress(final ProgressMonitor progressMonitor, final int progress)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressMonitor.setNote(String.format("%.01f%% Complete", (float)progress / progressMonitor.getMaximum() * 100));
                progressMonitor.setProgress(progress);
            }
        });
    }
    
    private void checkWorkspace()
    {
        validWorkspace = DREditor.workspaceRaw.exists() 
                            && DREditor.workspaceSrc.exists() 
                            && DREditor.workspaceTrans.exists()
                            && DREditor.rawEBOOT.exists()
                            && new File(DREditor.workspaceRaw, UmdPAKFile.UMDIMAGE.name).exists()
                            && new File(DREditor.workspaceRaw, UmdPAKFile.UMDIMAGE2.name).exists();
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                unpackLINFile.setEnabled(validWorkspace);
                pack.setEnabled(validWorkspace);
                buildISO.setEnabled(validWorkspace);
                textPreview.setEnabled(validWorkspace);
                
                if(validWorkspace)
                {
                    setModel(umdimageModel, UmdPAKFile.UMDIMAGE);
                    umdimageSorter.modelStructureChanged();
                    setModel(umdimage2Model, UmdPAKFile.UMDIMAGE2);
                    umdimage2Sorter.modelStructureChanged();
                }
            }
        });
    }
    
    private void updateTextPV()
    {
        if(BitmapFont.FONT1 != null)
        {
            exec.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    textPVImage = BitmapFont.FONT1.toImage(textPVArea.getText());
                    textPVPanel.repaint();
                }
            });
        }
    }
    
    private void updateBuildPV()
    {
        exec.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    buildPVImage = GUIUtils.generateStartupImage(328, 164, buildVersion.getText(), buildAuthors.getText(), buildComments.getText());
                    buildPVPanel.repaint();
                }
                catch(IOException e)
                {
                    buildPVImage = null;
                }
            }
        });
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        workspaceChooser = new javax.swing.JFileChooser();
        isoChooser = new javax.swing.JFileChooser();
        fontDialog = new javax.swing.JFileChooser();
        confirmGroup = new javax.swing.ButtonGroup();
        convertGroup = new javax.swing.ButtonGroup();
        settingsDialog = new javax.swing.JDialog();
        jPanel4 = new javax.swing.JPanel();
        langCombo = new javax.swing.JComboBox();
        langLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        confirmX = new javax.swing.JRadioButton();
        confirmLabel = new javax.swing.JLabel();
        confirmO = new javax.swing.JRadioButton();
        jPanel6 = new javax.swing.JPanel();
        umdimage = new javax.swing.JCheckBox();
        filesLabel = new javax.swing.JLabel();
        umdimage2 = new javax.swing.JCheckBox();
        packCancel = new javax.swing.JButton();
        packButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        buildScreen = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        buildVersion = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        buildAuthors = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        buildComments = new javax.swing.JTextArea();
        buildPVPanel = new javax.swing.JPanel(){
            @Override
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                if(buildPVImage != null)
                g.drawImage(buildPVImage, 0, 0, null);
            }
        };
        unpackFilesDialog = new javax.swing.JDialog();
        umdimageFiles = new javax.swing.JScrollPane();
        umdimageFileTable = new javax.swing.JTable(){
            @Override
            public Class getColumnClass(int column)
            {
                switch(column)
                {
                    case 0:     return(Boolean.class);
                    case 1:     return(Integer.class);
                    default:    return(String.class);
                }
            }
        };
        umdimage2Files = new javax.swing.JScrollPane();
        umdimage2FileTable = new javax.swing.JTable(){
            @Override
            public Class getColumnClass(int column)
            {
                switch(column)
                {
                    case 0:     return(Boolean.class);
                    case 1:     return(Integer.class);
                    default:    return(String.class);
                }
            }
        };
        umdimageAll = new javax.swing.JButton();
        umdimageNone = new javax.swing.JButton();
        umdimageInvert = new javax.swing.JButton();
        unpackFileSearchLabel = new javax.swing.JLabel();
        unpackFileSearch = new javax.swing.JTextField();
        unpackCancel = new javax.swing.JButton();
        unpackButton = new javax.swing.JButton();
        umdimage2All = new javax.swing.JButton();
        umdimage2None = new javax.swing.JButton();
        umdimage2Invert = new javax.swing.JButton();
        convertGIM = new javax.swing.JCheckBox();
        unpackEBOOTStrings = new javax.swing.JCheckBox();
        textPVDialog = new javax.swing.JDialog();
        textPVPanel = new javax.swing.JPanel(){
            @Override
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.drawImage(textPVImage, 0, 0, null);
            }
        };
        jScrollPane1 = new javax.swing.JScrollPane();
        textPVArea = new javax.swing.JTextArea();
        textPVClose = new javax.swing.JButton();
        workspaceLabel = new javax.swing.JLabel();
        workspaceField = new javax.swing.JTextField();
        browse = new javax.swing.JButton();
        statusBar = new javax.swing.JTextField();
        statusLabel = new javax.swing.JLabel();
        actionsPanel = new javax.swing.JPanel();
        unpackFromISO = new javax.swing.JButton();
        unpackLINFile = new javax.swing.JButton();
        pack = new javax.swing.JButton();
        buildISO = new javax.swing.JButton();
        toolsPanel = new javax.swing.JPanel();
        textPreview = new javax.swing.JButton();
        changeFont = new javax.swing.JButton();
        isoLabel = new javax.swing.JLabel();
        isoField = new javax.swing.JTextField();
        isoBrowse = new javax.swing.JButton();

        workspaceChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("dreditor/gui/Bundle"); // NOI18N
        workspaceChooser.setDialogTitle(bundle.getString("GUI.workspaceChooser.dialogTitle")); // NOI18N
        workspaceChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        workspaceChooser.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                workspaceChooserPropertyChange(evt);
            }
        });

        isoChooser.setDialogTitle(bundle.getString("GUI.isoChooser.dialogTitle")); // NOI18N
        isoChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("UMD ISO File", "iso"));
        isoChooser.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                isoChooserPropertyChange(evt);
            }
        });

        fontDialog.setDialogTitle(bundle.getString("GUI.fontDialog.dialogTitle")); // NOI18N
        fontDialog.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("BMFont XML Font File", "fnt"));

        settingsDialog.setTitle(bundle.getString("GUI.settingsDialog.title")); // NOI18N
        settingsDialog.setLocationByPlatform(true);
        settingsDialog.setModal(true);
        settingsDialog.setResizable(false);

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        langCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Japanese", "English", "French", "Spanish", "German", "Italian", "Dutch", "Portuguese", "Russian", "Korean", "Traditional Chinese", "Simplified Chinese" }));

        langLabel.setText(bundle.getString("GUI.langLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(langLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(langCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(langLabel)
                    .addComponent(langCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        confirmGroup.add(confirmX);
        confirmX.setText(bundle.getString("GUI.confirmX.text")); // NOI18N

        confirmLabel.setText(bundle.getString("GUI.confirmLabel.text")); // NOI18N

        confirmGroup.add(confirmO);
        confirmO.setSelected(true);
        confirmO.setText(bundle.getString("GUI.confirmO.text")); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(confirmX)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(confirmLabel)
                        .addGap(27, 27, 27)
                        .addComponent(confirmO)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confirmLabel)
                    .addComponent(confirmO))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(confirmX)
                .addContainerGap())
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        umdimage.setText(bundle.getString("GUI.umdimage.text")); // NOI18N

        filesLabel.setText(bundle.getString("GUI.filesLabel.text")); // NOI18N

        umdimage2.setText(bundle.getString("GUI.umdimage2.text")); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(umdimage)
                    .addComponent(umdimage2))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filesLabel)
                    .addComponent(umdimage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(umdimage2)
                .addContainerGap())
        );

        packCancel.setText(bundle.getString("GUI.packCancel.text")); // NOI18N
        packCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                packCancelActionPerformed(evt);
            }
        });

        packButton.setText(bundle.getString("GUI.packButton.text")); // NOI18N
        packButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                packButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buildScreen.setText(bundle.getString("GUI.buildScreen.text")); // NOI18N

        jLabel1.setText(bundle.getString("GUI.jLabel1.text")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel2.setText(bundle.getString("GUI.jLabel2.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel2, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel3.setText(bundle.getString("GUI.jLabel3.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel3, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        buildVersion.setText(bundle.getString("GUI.buildVersion.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), buildVersion, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        buildAuthors.setColumns(20);
        buildAuthors.setRows(3);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), buildAuthors, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jScrollPane2.setViewportView(buildAuthors);

        buildComments.setColumns(20);
        buildComments.setRows(3);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, buildScreen, org.jdesktop.beansbinding.ELProperty.create("${selected}"), buildComments, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jScrollPane3.setViewportView(buildComments);

        buildPVPanel.setBackground(new java.awt.Color(0, 0, 0));
        buildPVPanel.setPreferredSize(new java.awt.Dimension(328, 164));

        javax.swing.GroupLayout buildPVPanelLayout = new javax.swing.GroupLayout(buildPVPanel);
        buildPVPanel.setLayout(buildPVPanelLayout);
        buildPVPanelLayout.setHorizontalGroup(
            buildPVPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 328, Short.MAX_VALUE)
        );
        buildPVPanelLayout.setVerticalGroup(
            buildPVPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 164, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buildVersion)
                            .addComponent(jScrollPane2)
                            .addComponent(jScrollPane3)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buildScreen)
                            .addComponent(buildPVPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buildScreen, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(buildVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buildPVPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout settingsDialogLayout = new javax.swing.GroupLayout(settingsDialog.getContentPane());
        settingsDialog.getContentPane().setLayout(settingsDialogLayout);
        settingsDialogLayout.setHorizontalGroup(
            settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(packButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(packCancel))
                    .addGroup(settingsDialogLayout.createSequentialGroup()
                        .addGroup(settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        settingsDialogLayout.setVerticalGroup(
            settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsDialogLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(settingsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(packCancel)
                            .addComponent(packButton)))
                    .addGroup(settingsDialogLayout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        unpackFilesDialog.setTitle(bundle.getString("GUI.unpackFilesDialog.title")); // NOI18N
        unpackFilesDialog.setModal(true);
        unpackFilesDialog.setResizable(false);

        umdimageFiles.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), bundle.getString("GUI.umdimageFiles.border.title"))); // NOI18N

        umdimageFileTable.setModel(umdimageModel);
        umdimageFileTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        umdimageFileTable.setRowSorter(umdimageSorter);
        umdimageFiles.setViewportView(umdimageFileTable);

        umdimage2Files.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), bundle.getString("GUI.umdimage2Files.border.title"))); // NOI18N

        umdimage2FileTable.setModel(umdimage2Model);
        umdimage2FileTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        umdimage2FileTable.setRowSorter(umdimage2Sorter);
        umdimage2Files.setViewportView(umdimage2FileTable);

        umdimageAll.setText(bundle.getString("GUI.umdimageAll.text")); // NOI18N
        umdimageAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimageAllActionPerformed(evt);
            }
        });

        umdimageNone.setText(bundle.getString("GUI.umdimageNone.text")); // NOI18N
        umdimageNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimageNoneActionPerformed(evt);
            }
        });

        umdimageInvert.setText(bundle.getString("GUI.umdimageInvert.text")); // NOI18N
        umdimageInvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimageInvertActionPerformed(evt);
            }
        });

        unpackFileSearchLabel.setText(bundle.getString("GUI.unpackFileSearchLabel.text")); // NOI18N

        unpackFileSearch.setText(bundle.getString("GUI.unpackFileSearch.text")); // NOI18N

        unpackCancel.setText(bundle.getString("GUI.unpackCancel.text")); // NOI18N
        unpackCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unpackCancelActionPerformed(evt);
            }
        });

        unpackButton.setText(bundle.getString("GUI.unpackButton.text")); // NOI18N
        unpackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unpackButtonActionPerformed(evt);
            }
        });

        umdimage2All.setText(bundle.getString("GUI.umdimage2All.text")); // NOI18N
        umdimage2All.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimage2AllActionPerformed(evt);
            }
        });

        umdimage2None.setText(bundle.getString("GUI.umdimage2None.text")); // NOI18N
        umdimage2None.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimage2NoneActionPerformed(evt);
            }
        });

        umdimage2Invert.setText(bundle.getString("GUI.umdimage2Invert.text")); // NOI18N
        umdimage2Invert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdimage2InvertActionPerformed(evt);
            }
        });

        convertGIM.setText(bundle.getString("GUI.convertGIM.text")); // NOI18N

        unpackEBOOTStrings.setText(bundle.getString("GUI.unpackEBOOTStrings.text")); // NOI18N

        javax.swing.GroupLayout unpackFilesDialogLayout = new javax.swing.GroupLayout(unpackFilesDialog.getContentPane());
        unpackFilesDialog.getContentPane().setLayout(unpackFilesDialogLayout);
        unpackFilesDialogLayout.setHorizontalGroup(
            unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(unpackFilesDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, unpackFilesDialogLayout.createSequentialGroup()
                        .addComponent(unpackFileSearchLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unpackFileSearch))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, unpackFilesDialogLayout.createSequentialGroup()
                        .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(umdimageFiles, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(unpackFilesDialogLayout.createSequentialGroup()
                                .addComponent(umdimageAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(umdimageNone)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(umdimageInvert)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(unpackFilesDialogLayout.createSequentialGroup()
                                .addComponent(umdimage2All)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(umdimage2None)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(umdimage2Invert)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(umdimage2Files)))
                    .addGroup(unpackFilesDialogLayout.createSequentialGroup()
                        .addComponent(convertGIM)
                        .addGap(18, 18, 18)
                        .addComponent(unpackEBOOTStrings)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(unpackButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unpackCancel)))
                .addContainerGap())
        );
        unpackFilesDialogLayout.setVerticalGroup(
            unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, unpackFilesDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(umdimageFiles, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                    .addComponent(umdimage2Files, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(umdimage2All)
                        .addComponent(umdimage2None)
                        .addComponent(umdimage2Invert))
                    .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(umdimageAll)
                        .addComponent(umdimageNone)
                        .addComponent(umdimageInvert)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unpackFileSearchLabel)
                    .addComponent(unpackFileSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(unpackFilesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unpackCancel)
                    .addComponent(unpackButton)
                    .addComponent(convertGIM)
                    .addComponent(unpackEBOOTStrings))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        textPVDialog.setTitle(bundle.getString("GUI.textPVDialog.title")); // NOI18N
        textPVDialog.setModal(true);
        textPVDialog.setResizable(false);

        textPVPanel.setBackground(new java.awt.Color(0, 0, 0));
        textPVPanel.setPreferredSize(new java.awt.Dimension(480, 72));

        javax.swing.GroupLayout textPVPanelLayout = new javax.swing.GroupLayout(textPVPanel);
        textPVPanel.setLayout(textPVPanelLayout);
        textPVPanelLayout.setHorizontalGroup(
            textPVPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        textPVPanelLayout.setVerticalGroup(
            textPVPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 72, Short.MAX_VALUE)
        );

        textPVArea.setColumns(20);
        textPVArea.setFont(new java.awt.Font("Arial Unicode MS", 0, 14)); // NOI18N
        textPVArea.setLineWrap(true);
        textPVArea.setRows(2);
        jScrollPane1.setViewportView(textPVArea);

        textPVClose.setText(bundle.getString("GUI.textPVClose.text")); // NOI18N
        textPVClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textPVCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout textPVDialogLayout = new javax.swing.GroupLayout(textPVDialog.getContentPane());
        textPVDialog.getContentPane().setLayout(textPVDialogLayout);
        textPVDialogLayout.setHorizontalGroup(
            textPVDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(textPVDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(textPVDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(textPVDialogLayout.createSequentialGroup()
                        .addGroup(textPVDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                            .addComponent(textPVPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, textPVDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(textPVClose)))
                .addContainerGap())
        );
        textPVDialogLayout.setVerticalGroup(
            textPVDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(textPVDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(textPVPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textPVClose)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(bundle.getString("GUI.title")); // NOI18N
        setResizable(false);

        workspaceLabel.setText(bundle.getString("GUI.workspaceLabel.text")); // NOI18N

        workspaceField.setEditable(false);

        browse.setText(bundle.getString("GUI.browse.text")); // NOI18N
        browse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonClicked(evt);
            }
        });

        statusBar.setEditable(false);
        statusBar.setText(bundle.getString("GUI.statusBar.text")); // NOI18N

        statusLabel.setText(bundle.getString("GUI.statusLabel.text")); // NOI18N

        actionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), bundle.getString("GUI.actionsPanel.border.title"))); // NOI18N

        unpackFromISO.setText(bundle.getString("GUI.unpackFromISO.text")); // NOI18N
        unpackFromISO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unpackFromISOActionPerformed(evt);
            }
        });

        unpackLINFile.setText(bundle.getString("GUI.unpackLINFile.text")); // NOI18N
        unpackLINFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unpackLINFileActionPerformed(evt);
            }
        });

        pack.setText(bundle.getString("GUI.pack.text")); // NOI18N
        pack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                packActionPerformed(evt);
            }
        });

        buildISO.setText(bundle.getString("GUI.buildISO.text")); // NOI18N
        buildISO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buildISOActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout actionsPanelLayout = new javax.swing.GroupLayout(actionsPanel);
        actionsPanel.setLayout(actionsPanelLayout);
        actionsPanelLayout.setHorizontalGroup(
            actionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buildISO, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(actionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(actionsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(actionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(unpackLINFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(unpackFromISO, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pack, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE))
                    .addContainerGap()))
        );
        actionsPanelLayout.setVerticalGroup(
            actionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, actionsPanelLayout.createSequentialGroup()
                .addContainerGap(96, Short.MAX_VALUE)
                .addComponent(buildISO)
                .addContainerGap())
            .addGroup(actionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(actionsPanelLayout.createSequentialGroup()
                    .addGap(9, 9, 9)
                    .addComponent(unpackFromISO)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(unpackLINFile)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(pack)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        toolsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), bundle.getString("GUI.toolsPanel.border.title"))); // NOI18N

        textPreview.setText(bundle.getString("GUI.textPreview.text")); // NOI18N
        textPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textPreviewActionPerformed(evt);
            }
        });

        changeFont.setText(bundle.getString("GUI.changeFont.text")); // NOI18N
        changeFont.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeFontActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout toolsPanelLayout = new javax.swing.GroupLayout(toolsPanel);
        toolsPanel.setLayout(toolsPanelLayout);
        toolsPanelLayout.setHorizontalGroup(
            toolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                    .addComponent(changeFont, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        toolsPanelLayout.setVerticalGroup(
            toolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(textPreview)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(changeFont)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        isoLabel.setText(bundle.getString("GUI.isoLabel.text")); // NOI18N

        isoField.setEditable(false);
        isoField.setText(bundle.getString("GUI.isoField.text")); // NOI18N

        isoBrowse.setText(bundle.getString("GUI.isoBrowse.text")); // NOI18N
        isoBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                isoBrowseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(toolsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(actionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(statusLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusBar))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(isoLabel)
                            .addComponent(workspaceLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(isoField)
                            .addComponent(workspaceField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(browse)
                            .addComponent(isoBrowse))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(isoLabel)
                    .addComponent(isoField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(isoBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(workspaceLabel)
                    .addComponent(workspaceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(toolsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusLabel))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonClicked
        workspaceChooser.showSaveDialog(this);
    }//GEN-LAST:event_browseButtonClicked

    private void unpackFromISOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unpackFromISOActionPerformed
        final File isoFile = getISOFile();
        
        if(isoFile != null)
        {
            final ProgressMonitor progressMonitor = 
                new ProgressMonitor(this,
                    "Unpacking from ISO...", "", 0, 3);
            statusBar.setText(GUIUtils.BUNDLE.getString("GUI.unpacking"));
            new Thread()
            {
                int progress = 0;
                
                @Override
                public void run()
                {
                    try
                    {
                        Runnable increment = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(progressMonitor.isCanceled())
                                    throw new RuntimeException("Canceled");
                                setProgress(progressMonitor, ++progress);
                            }
                        };
                        
                        DREditor.unpackFromISO(isoFile, increment);
                        setStatus(GUIUtils.BUNDLE.getString("GUI.finishedUnpacking"));
                        
                        checkWorkspace();
                    }
                    catch(IOException ioe)
                    {
                        setStatus(GUIUtils.BUNDLE.getString("Error.IOException"));
                        GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
                    }
                    catch(RuntimeException e)
                    {
                        setStatus(e.getMessage());
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressMonitor.close();
                            }
                        });
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_unpackFromISOActionPerformed

    private void unpackLINFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unpackLINFileActionPerformed
        convertGIM.setSelected(config.CONVERT_GIM);
        
        for(int i = 0; i < umdimageModel.getRowCount(); i++)
            umdimageModel.setValueAt(false, i, 0);
        for(int i = 0; i < umdimage2Model.getRowCount(); i++)
            umdimage2Model.setValueAt(false, i, 0);
        
        unpackFilesDialog.setVisible(true);
    }//GEN-LAST:event_unpackLINFileActionPerformed

    private void packActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_packActionPerformed
        settingsDialog.setVisible(true);
    }//GEN-LAST:event_packActionPerformed

    private void packCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_packCancelActionPerformed
        settingsDialog.setVisible(false);
        loadSettings();
    }//GEN-LAST:event_packCancelActionPerformed

    private void packButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_packButtonActionPerformed
        if(saveSettings())
        {
            final int count = (config.PACK_UMDIMAGE ? UmdPAKFile.UMDIMAGE.numFiles : 0)
                                + (config.PACK_UMDIMAGE2 ? UmdPAKFile.UMDIMAGE2.numFiles : 0);
            final ProgressMonitor progressMonitor = 
                new ProgressMonitor(settingsDialog,
                    "Packing Files...", "", 0, count);
            
            new Thread()
            {
                int progress = 0;
                
                @Override
                public void run()
                {
                    try
                    {
                        Runnable increment = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(progressMonitor.isCanceled())
                                    throw new RuntimeException("Canceled");
                                setProgress(progressMonitor, ++progress);
                            }
                        };
                        
                        DREditor.prepareEBOOT(config);
                        if(config.PACK_UMDIMAGE)
                            DREditor.pack(config, UmdPAKFile.UMDIMAGE, increment);
                        if(config.PACK_UMDIMAGE2)
                            DREditor.pack(config, UmdPAKFile.UMDIMAGE2, increment);
                        
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                settingsDialog.setVisible(false);
                                statusBar.setText("Packing complete!");
                            }
                        });
                    }
                    catch(IOException ioe)
                    {
                        setStatus(GUIUtils.BUNDLE.getString("Error.IOException"));
                        GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
                        ioe.printStackTrace();
                    }
                    catch(InvalidTOCException ite)
                    {
                        setStatus(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
                        GUIUtils.error(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
                        ite.printStackTrace();
                    }
                    catch(RuntimeException e)
                    {
                        setStatus(e.getMessage());
                        e.printStackTrace();
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressMonitor.close();
                            }
                        });
                    }
                }
            }.start();
            loadSettings();
        }
    }//GEN-LAST:event_packButtonActionPerformed

    private void umdimageAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimageAllActionPerformed
        for(int i = 0; i < umdimageModel.getRowCount(); i++)
            if(umdimageSorter.convertRowIndexToView(i) != -1)
                umdimageModel.setValueAt(true, i, 0);
    }//GEN-LAST:event_umdimageAllActionPerformed

    private void umdimageNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimageNoneActionPerformed
        for(int i = 0; i < umdimageModel.getRowCount(); i++)
            if(umdimageSorter.convertRowIndexToView(i) != -1)
                umdimageModel.setValueAt(false, i, 0);
    }//GEN-LAST:event_umdimageNoneActionPerformed

    private void umdimageInvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimageInvertActionPerformed
        for(int i = 0; i < umdimageModel.getRowCount(); i++)
            if(umdimageSorter.convertRowIndexToView(i) != -1)
                umdimageModel.setValueAt(!((Boolean)umdimageModel.getValueAt(i, 0)), i, 0);
    }//GEN-LAST:event_umdimageInvertActionPerformed

    private void unpackCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unpackCancelActionPerformed
        unpackFilesDialog.setVisible(false);
    }//GEN-LAST:event_unpackCancelActionPerformed

    private void umdimage2AllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimage2AllActionPerformed
        for(int i = 0; i < umdimage2Model.getRowCount(); i++)
            if(umdimage2Sorter.convertRowIndexToView(i) != -1)
                umdimage2Model.setValueAt(true, i, 0);
    }//GEN-LAST:event_umdimage2AllActionPerformed

    private void umdimage2NoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimage2NoneActionPerformed
        for(int i = 0; i < umdimage2Model.getRowCount(); i++)
            if(umdimage2Sorter.convertRowIndexToView(i) != -1)
                umdimage2Model.setValueAt(false, i, 0);
    }//GEN-LAST:event_umdimage2NoneActionPerformed

    private void umdimage2InvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdimage2InvertActionPerformed
        for(int i = 0; i < umdimage2Model.getRowCount(); i++)
            if(umdimage2Sorter.convertRowIndexToView(i) != -1)
                umdimage2Model.setValueAt(!((Boolean)umdimage2Model.getValueAt(i, 0)), i, 0);
    }//GEN-LAST:event_umdimage2InvertActionPerformed

    private void unpackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unpackButtonActionPerformed
        config.CONVERT_GIM = convertGIM.isSelected();
        
        int count = 0;
        for(int i = 0; i < umdimageModel.getRowCount(); i++)
            if((Boolean)umdimageModel.getValueAt(i, 0))
                count++;
        for(int i = 0; i < umdimage2Model.getRowCount(); i++)
            if((Boolean)umdimage2Model.getValueAt(i, 0))
                count++;
        
        final ProgressMonitor progressMonitor = 
                new ProgressMonitor(unpackFilesDialog,
                    "Unpacking Files...", "", 0, count);
        
        new Thread()
        {
            int progress = 0;
            
            @Override
            public void run()
            {
                try
                {
                    if(unpackEBOOTStrings.isSelected())
                        DREditor.unpackEBOOTStrings();
                    for(int i = 0; i < umdimageModel.getRowCount(); i++)
                    {
                        if((Boolean)umdimageModel.getValueAt(i, 0))
                        {
                            if(progressMonitor.isCanceled())
                            {
                                setStatus("Canceled");
                                return;
                            }
                            DREditor.unpack(config, UmdPAKFile.UMDIMAGE, i);
                            setProgress(progressMonitor, ++progress);
                        }
                    }
                    for(int i = 0; i < umdimage2Model.getRowCount(); i++)
                    {
                        if((Boolean)umdimage2Model.getValueAt(i, 0))
                        {
                            if(progressMonitor.isCanceled())
                            {
                                setStatus("Canceled");
                                return;
                            }
                            DREditor.unpack(config, UmdPAKFile.UMDIMAGE2, i);
                            setProgress(progressMonitor, ++progress);
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            unpackFilesDialog.setVisible(false);
                            statusBar.setText("Unpacking complete!");
                        }
                    });
                }
                catch(IOException ioe)
                {
                    setStatus(GUIUtils.BUNDLE.getString("Error.IOException"));
                    GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
                }
                catch(InvalidTOCException e)
                {
                    setStatus(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
                    GUIUtils.error(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
                }
                catch(org.json.JSONException js)
                {
                    setStatus(GUIUtils.BUNDLE.getString("Error.JSONException"));
                    GUIUtils.error(GUIUtils.BUNDLE.getString("Error.JSONException"));
                }
                catch(RuntimeException e)
                {
                    setStatus(e.getMessage());
                }
                finally
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressMonitor.close();
                        }
                    });
                }
            }
        }.start();
    }//GEN-LAST:event_unpackButtonActionPerformed

    private void textPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textPreviewActionPerformed
        try
        {
            // Init every time, because the font may have changed.
            BitmapFont.init(config);
            textPVDialog.setVisible(true);
        }
        catch(IOException ioe)
        {
            statusBar.setText(GUIUtils.BUNDLE.getString("Error.IOException"));
            GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
        }
        catch(InvalidTOCException e)
        {
            statusBar.setText(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
            GUIUtils.error(GUIUtils.BUNDLE.getString("Error.InvalidTOCException"));
        }
    }//GEN-LAST:event_textPreviewActionPerformed

    private void textPVCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textPVCloseActionPerformed
        textPVDialog.setVisible(false);
    }//GEN-LAST:event_textPVCloseActionPerformed

    private void workspaceChooserPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_workspaceChooserPropertyChange
        if(workspaceChooser.getSelectedFile() != null)
        {
            workspaceField.setText(workspaceChooser.getSelectedFile().getAbsolutePath());
            DREditor.setWorkspace(workspaceChooser.getSelectedFile());
            PrefsUtils.PREFS.put("dir", workspaceChooser.getSelectedFile().getAbsolutePath());

            checkWorkspace();
        }
    }//GEN-LAST:event_workspaceChooserPropertyChange

    private void isoChooserPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_isoChooserPropertyChange
        if(isoChooser.getSelectedFile() != null)
        {
            isoField.setText(isoChooser.getSelectedFile().getAbsolutePath());
            PrefsUtils.PREFS.put("iso", isoChooser.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_isoChooserPropertyChange

    private void isoBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_isoBrowseActionPerformed
        isoChooser.showOpenDialog(this);
    }//GEN-LAST:event_isoBrowseActionPerformed

    private void buildISOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buildISOActionPerformed
        final File isoFile = getISOFile();
        
        if(isoFile != null)
        {
            final ProgressMonitor progressMonitor = 
                new ProgressMonitor(this,
                    "Building ISO...", "", 0, ISOFiles.values().length);
            statusBar.setText(GUIUtils.BUNDLE.getString("GUI.buildingISO"));
            new Thread()
            {
                int progress = 0;
                
                @Override
                public void run()
                {
                    try
                    {
                        Runnable increment = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(progressMonitor.isCanceled())
                                    throw new RuntimeException("Canceled");
                                setProgress(progressMonitor, ++progress);
                            }
                        };
                        
                        DREditor.packToISO(isoFile, new File(DREditor.workspace, "build.iso"), increment);
                        
                        setStatus("Building complete!");
                    }
                    catch(IOException ioe)
                    {
                        setStatus(GUIUtils.BUNDLE.getString("Error.IOException"));
                        GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
                    }
                    catch(RuntimeException e)
                    {
                        setStatus(e.getMessage());
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressMonitor.close();
                            }
                        });
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_buildISOActionPerformed

    private void changeFontActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeFontActionPerformed
        try
        {
            if(fontDialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                DREditor.importFont(config, fontDialog.getSelectedFile());
                setStatus("Font changed!");
            }
        }
        catch(IOException|InvalidTOCException|JSONException|javax.xml.parsers.ParserConfigurationException|org.xml.sax.SAXException e)
        {
            setStatus(GUIUtils.BUNDLE.getString("Error.IOException"));
            GUIUtils.error(GUIUtils.BUNDLE.getString("Error.IOException"));
            e.printStackTrace();
        }
        catch(IllegalArgumentException iae)
        {
            setStatus(iae.getMessage());
        }
    }//GEN-LAST:event_changeFontActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new GUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionsPanel;
    private javax.swing.JButton browse;
    private javax.swing.JTextArea buildAuthors;
    private javax.swing.JTextArea buildComments;
    private javax.swing.JButton buildISO;
    private javax.swing.JPanel buildPVPanel;
    private javax.swing.JCheckBox buildScreen;
    private javax.swing.JTextField buildVersion;
    private javax.swing.JButton changeFont;
    private javax.swing.ButtonGroup confirmGroup;
    private javax.swing.JLabel confirmLabel;
    private javax.swing.JRadioButton confirmO;
    private javax.swing.JRadioButton confirmX;
    private javax.swing.JCheckBox convertGIM;
    private javax.swing.ButtonGroup convertGroup;
    private javax.swing.JLabel filesLabel;
    private javax.swing.JFileChooser fontDialog;
    private javax.swing.JButton isoBrowse;
    private javax.swing.JFileChooser isoChooser;
    private javax.swing.JTextField isoField;
    private javax.swing.JLabel isoLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JComboBox langCombo;
    private javax.swing.JLabel langLabel;
    private javax.swing.JButton pack;
    private javax.swing.JButton packButton;
    private javax.swing.JButton packCancel;
    private javax.swing.JDialog settingsDialog;
    private javax.swing.JTextField statusBar;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTextArea textPVArea;
    private javax.swing.JButton textPVClose;
    private javax.swing.JDialog textPVDialog;
    private javax.swing.JPanel textPVPanel;
    private javax.swing.JButton textPreview;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JCheckBox umdimage;
    private javax.swing.JCheckBox umdimage2;
    private javax.swing.JButton umdimage2All;
    private javax.swing.JTable umdimage2FileTable;
    private javax.swing.JScrollPane umdimage2Files;
    private javax.swing.JButton umdimage2Invert;
    private javax.swing.JButton umdimage2None;
    private javax.swing.JButton umdimageAll;
    private javax.swing.JTable umdimageFileTable;
    private javax.swing.JScrollPane umdimageFiles;
    private javax.swing.JButton umdimageInvert;
    private javax.swing.JButton umdimageNone;
    private javax.swing.JButton unpackButton;
    private javax.swing.JButton unpackCancel;
    private javax.swing.JCheckBox unpackEBOOTStrings;
    private javax.swing.JTextField unpackFileSearch;
    private javax.swing.JLabel unpackFileSearchLabel;
    private javax.swing.JDialog unpackFilesDialog;
    private javax.swing.JButton unpackFromISO;
    private javax.swing.JButton unpackLINFile;
    private javax.swing.JFileChooser workspaceChooser;
    private javax.swing.JTextField workspaceField;
    private javax.swing.JLabel workspaceLabel;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
