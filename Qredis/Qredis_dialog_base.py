# -*- coding: utf-8 -*-

# Form implementation generated from reading ui file 'Qredis_dialog_base.ui'
#
# Created by: PyQt5 UI code generator 5.15.10
#
# WARNING: Any manual changes made to this file will be lost when pyuic5 is
# run again.  Do not edit this file unless you know what you are doing.


from PyQt5 import QtCore, QtGui, QtWidgets


class Ui_QredisDialogBase(object):
    def setupUi(self, QredisDialogBase):
        QredisDialogBase.setObjectName("QredisDialogBase")
        QredisDialogBase.resize(269, 119)
        self.toolButton = QtWidgets.QToolButton(QredisDialogBase)
        self.toolButton.setGeometry(QtCore.QRect(30, 40, 71, 31))
        self.toolButton.setObjectName("toolButton")
        self.pushButton = QtWidgets.QPushButton(QredisDialogBase)
        self.pushButton.setGeometry(QtCore.QRect(140, 40, 93, 28))
        self.pushButton.setObjectName("pushButton")

        self.retranslateUi(QredisDialogBase)
        QtCore.QMetaObject.connectSlotsByName(QredisDialogBase)

    def retranslateUi(self, QredisDialogBase):
        _translate = QtCore.QCoreApplication.translate
        QredisDialogBase.setWindowTitle(_translate("QredisDialogBase", "Qredis"))
        self.toolButton.setText(_translate("QredisDialogBase", "..."))
        self.pushButton.setText(_translate("QredisDialogBase", "Upload"))