// ClientDlg.h : header file
//

#include "afxcmn.h"
#include "afxwin.h"
#if !defined(AFX_CLIENTDLG_H__6E81449C_EFAC_45BD_A835_0DCD2B57292F__INCLUDED_)
#define AFX_CLIENTDLG_H__6E81449C_EFAC_45BD_A835_0DCD2B57292F__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

/////////////////////////////////////////////////////////////////////////////
// CClientDlg dialog

class CClientDlg : public CDialog
{
// Construction
public:
	CClientDlg(CWnd* pParent = NULL);	// standard constructor

// Dialog Data
	//{{AFX_DATA(CClientDlg)
	enum { IDD = IDD_CLIENT_DIALOG };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CClientDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	HICON m_hIcon;

	// Generated message map functions
	//{{AFX_MSG(CClientDlg)
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	virtual void OnOK();
//	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	CSliderCtrl Sl;
	CSliderCtrl slideLevel;
	afx_msg void OnNMCustomdrawSliderQoe(NMHDR *pNMHDR, LRESULT *pResult);
	CListBox ListBox;
	afx_msg void OnLbnSelchangeList();
	afx_msg void OnBnClickedButtonUpdatelist();
	afx_msg void OnNMCustomdrawSlider1(NMHDR *pNMHDR, LRESULT *pResult);
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_CLIENTDLG_H__6E81449C_EFAC_45BD_A835_0DCD2B57292F__INCLUDED_)
