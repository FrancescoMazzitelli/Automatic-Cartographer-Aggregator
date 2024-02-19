# -*- coding: utf-8 -*-
"""
/***************************************************************************
 Qredis
                                 A QGIS plugin
 Fast way to send files to redis
 Generated by Plugin Builder: http://g-sherman.github.io/Qgis-Plugin-Builder/
                              -------------------
        begin                : 2024-01-18
        git sha              : $Format:%H$
        copyright            : (C) 2024 by Francesco Cosimo Mazzitelli
        email                : francesco@qgis.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
"""
from qgis.PyQt.QtCore import QSettings, QTranslator, QCoreApplication
from qgis.core import QgsMessageLog, Qgis
from qgis.PyQt.QtGui import QIcon
from qgis.PyQt.QtWidgets import (
    QAction, QFileDialog, QDialog, QMessageBox, QVBoxLayout, QComboBox, 
    QDialogButtonBox, QLineEdit, QLabel, QRadioButton, QHBoxLayout
)
import requests
import geopandas as gpd
import json

# Initialize Qt resources from file resources.py
from .resources import *
# Import the code for the dialog
from .Qredis_dialog import QredisDialog
import os
import os.path

class CustomInputDialog(QDialog):
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle('Inserisci dati')

        layout = QVBoxLayout(self)

        # Aggiungi il combobox
        self.combo_box = QComboBox()
        self.combo_box.addItems(['Servizio di raccolta', 'Servizio di pulizia strade'])
        layout.addWidget(self.combo_box)

        # Aggiungi i radio button
        self.radio_layout = QHBoxLayout()
        self.threshold_radio = QRadioButton('Soglia')
        self.cluster_radio = QRadioButton('Numero Cluster')

        # Set one of the radio buttons as checked initially
        self.threshold_radio.setChecked(True)

        self.radio_layout.addWidget(self.threshold_radio)
        self.radio_layout.addWidget(self.cluster_radio)
        layout.addLayout(self.radio_layout)

        # Aggiungi il campo di testo a singola linea con la label personalizzata
        self.label_text = 'Inserisci Soglia: '
        self.threshold_label = QLabel(self.label_text)
        layout.addWidget(self.threshold_label)
        self.threshold_edit = QLineEdit()
        layout.addWidget(self.threshold_edit)

        # Aggiungi i bottoni OK e Annulla
        buttons_box = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel)
        layout.addWidget(buttons_box)

        buttons_box.accepted.connect(self.accept)
        buttons_box.rejected.connect(self.reject)

        # Connect radio buttons to update label
        self.threshold_radio.toggled.connect(lambda: self.update_label('Soglia'))
        self.cluster_radio.toggled.connect(lambda: self.update_label('Numero Cluster'))

    def update_label(self, label_text):
        self.label_text = f'Inserisci {label_text}:'
        self.threshold_label.setText(self.label_text)

    def get_values(self):
        return self.threshold_edit.text(), self.combo_box.currentText(), self.threshold_radio.isChecked(), self.cluster_radio.isChecked()

class Qredis:
    """QGIS Plugin Implementation."""

    def insert_to_geomesa(self):
        """Handler for inserting data to Geomesa."""
        try:
            # Mostra la finestra di input personalizzata
            input_dialog = CustomInputDialog(self.iface.mainWindow())
            result = input_dialog.exec_()

            if result == QDialog.Accepted:
                # Ottieni i valori dal campo di testo multilinea e dalla selezione del servizio
                threshold, selected_service, is_threshold_selected, is_cluster_selected = input_dialog.get_values()

                # Specifica l'URL del tuo endpoint Java
                url = "http://localhost:8000/insert"

                data = {
                        'threshold': str(threshold), 
                        'service': str(selected_service), 
                        'thresholdClustering': int(is_threshold_selected), 
                        'spectralClustering': int(is_cluster_selected)
                        }
                
                payload = json.dumps(data)

                headers = {'Content-Type': 'application/json'}

                # Invia una richiesta HTTP POST con la soglia utenza nel corpo
                response = requests.post(url=url, data=payload, headers=headers)

                # Controlla lo stato della risposta
                response.raise_for_status()

                QMessageBox.information(self.iface.mainWindow(), 'Successo', 'Dati inseriti con successo!')

        except Exception as e:
            QgsMessageLog.logMessage(f"Errore durante il caricamento su Redis: {str(e)}", 'Qredis', Qgis.Critical)
            QMessageBox.critical(self.iface.mainWindow(), 'Errore', f"Errore durante il caricamento su Geomesa: {str(e)}")

    def upload_shapefile_to_redis(self, shapefile_content, filename):
        """Carica lo shapefile su Geomesa."""

        url = "http://localhost:8000/upload"
        geojson_string = shapefile_content.to_json()

        data = {
            'filename': filename,
            'content': geojson_string,
        }
        
        try:
            requests.post(url=url, json=data)

        except Exception as e:
            QgsMessageLog.logMessage(f"Errore durante il caricamento su GeoMesa Gateway: {str(e)}", 'Qredis', Qgis.Critical)
            return False
        return True
     
    def select_shapefile(self):
        """Handler for selecting a shapefile."""

        # Apre una finestra di dialogo per la selezione di uno shapefile
        file_dialog = QFileDialog()
        file_dialog.setFileMode(QFileDialog.ExistingFile)
        file_dialog.setNameFilter("Shapefiles (*.shp)")
        file_dialog.setViewMode(QFileDialog.Detail)

        if file_dialog.exec_():
            # Ottieni il percorso del file selezionato
            shapefile_path = file_dialog.selectedFiles()[0]
            gdf = gpd.read_file(shapefile_path)
            filename, file_extension = os.path.splitext(os.path.basename(shapefile_path))

            QgsMessageLog.logMessage(f"Shapefile selezionato: {shapefile_path}", 'Qredis', Qgis.Info)

            success = self.upload_shapefile_to_redis(gdf, filename)

            if success:
                QgsMessageLog.logMessage("Shapefile caricato su GeoMesa Gateway con successo.", 'Qredis', Qgis.Info)
            else:
                QgsMessageLog.logMessage("Errore durante il caricamento dello shapefile su GeoMesa Gateway.", 'Qredis', Qgis.Warning)
                

    def __init__(self, iface):
        """Constructor.

        :param iface: An interface instance that will be passed to this class
            which provides the hook by which you can manipulate the QGIS
            application at run time.
        :type iface: QgsInterface
        """
        # Save reference to the QGIS interface
        self.iface = iface
        # initialize plugin directory
        self.plugin_dir = os.path.dirname(__file__)
        # initialize locale
        locale = QSettings().value('locale/userLocale')[0:2]
        locale_path = os.path.join(
            self.plugin_dir,
            'i18n',
            'Qredis_{}.qm'.format(locale))

        if os.path.exists(locale_path):
            self.translator = QTranslator()
            self.translator.load(locale_path)
            QCoreApplication.installTranslator(self.translator)

        # Declare instance attributes
        self.actions = []
        self.menu = self.tr(u'&Qredis')

        # Check if plugin was started the first time in current QGIS session
        # Must be set in initGui() to survive plugin reloads
        self.first_start = None

    # noinspection PyMethodMayBeStatic
    def tr(self, message):
        """Get the translation for a string using Qt translation API.

        We implement this ourselves since we do not inherit QObject.

        :param message: String for translation.
        :type message: str, QString

        :returns: Translated version of message.
        :rtype: QString
        """
        # noinspection PyTypeChecker,PyArgumentList,PyCallByClass
        return QCoreApplication.translate('Qredis', message)


    def add_action(
        self,
        icon_path,
        text,
        callback,
        enabled_flag=True,
        add_to_menu=True,
        add_to_toolbar=True,
        status_tip=None,
        whats_this=None,
        parent=None):
        """Add a toolbar icon to the toolbar.

        :param icon_path: Path to the icon for this action. Can be a resource
            path (e.g. ':/plugins/foo/bar.png') or a normal file system path.
        :type icon_path: str

        :param text: Text that should be shown in menu items for this action.
        :type text: str

        :param callback: Function to be called when the action is triggered.
        :type callback: function

        :param enabled_flag: A flag indicating if the action should be enabled
            by default. Defaults to True.
        :type enabled_flag: bool

        :param add_to_menu: Flag indicating whether the action should also
            be added to the menu. Defaults to True.
        :type add_to_menu: bool

        :param add_to_toolbar: Flag indicating whether the action should also
            be added to the toolbar. Defaults to True.
        :type add_to_toolbar: bool

        :param status_tip: Optional text to show in a popup when mouse pointer
            hovers over the action.
        :type status_tip: str

        :param parent: Parent widget for the new action. Defaults None.
        :type parent: QWidget

        :param whats_this: Optional text to show in the status bar when the
            mouse pointer hovers over the action.

        :returns: The action that was created. Note that the action is also
            added to self.actions list.
        :rtype: QAction
        """

        icon = QIcon(icon_path)
        action = QAction(icon, text, parent)
        action.triggered.connect(callback)
        action.setEnabled(enabled_flag)

        if status_tip is not None:
            action.setStatusTip(status_tip)

        if whats_this is not None:
            action.setWhatsThis(whats_this)

        if add_to_toolbar:
            # Adds plugin icon to Plugins toolbar
            self.iface.addToolBarIcon(action)

        if add_to_menu:
            self.iface.addPluginToMenu(
                self.menu,
                action)

        self.actions.append(action)

        return action

    def initGui(self):
        """Create the menu entries and toolbar icons inside the QGIS GUI."""

        icon_path = ':/plugins/Qredis/upload.png'
        self.add_action(
            icon_path,
            text=self.tr(u'Seleziona e carica uno shapefile'),
            callback=self.select_shapefile,
            parent=self.iface.mainWindow())
        
        icon_path_insert = ':/plugins/Qredis/compute.png'
        self.add_action(
            icon_path_insert,
            text=self.tr(u'Fai partire l\'elaborazione'),
            callback=self.insert_to_geomesa,
            parent=self.iface.mainWindow())

        # will be set False in run()
        self.first_start = True


    def unload(self):
        """Removes the plugin menu item and icon from QGIS GUI."""
        for action in self.actions:
            self.iface.removePluginMenu(
                self.tr(u'&Qredis'),
                action)
            self.iface.removeToolBarIcon(action)


    def run(self):
        """Run method that performs all the real work"""

        # Create the dialog with elements (after translation) and keep reference
        # Only create GUI ONCE in callback, so that it will only load when the plugin is started
        if self.first_start == True:
            self.first_start = False
            self.dlg = QredisDialog()

        # show the dialog
        self.dlg.show()
        # Run the dialog event loop
        result = self.dlg.exec_()
        # See if OK was pressed
        if result:
            # Do something useful here - delete the line containing pass and
            # substitute with your code.
            pass