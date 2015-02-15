// ClientDlg.cpp : implementation file
//

#include "stdafx.h"
#include "Client.h"
#include "ClientDlg.h"
#include <afxsock.h>  
#include <afxwin.h>


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CAboutDlg dialog used for App About

class CAboutDlg : public CDialog
{
public:
	CAboutDlg();

// Dialog Data
	//{{AFX_DATA(CAboutDlg)
	enum { IDD = IDD_ABOUTBOX };
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAboutDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	//{{AFX_MSG(CAboutDlg)
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialog(CAboutDlg::IDD)
{
	//{{AFX_DATA_INIT(CAboutDlg)
	//}}AFX_DATA_INIT
}

void CAboutDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CAboutDlg)
	//}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialog)
	//{{AFX_MSG_MAP(CAboutDlg)
		// No message handlers
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CClientDlg dialog

CClientDlg::CClientDlg(CWnd* pParent /*=NULL*/)
	: CDialog(CClientDlg::IDD, pParent)
{
	//{{AFX_DATA_INIT(CClientDlg)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	// Note that LoadIcon does not require a subsequent DestroyIcon in Win32
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
}

void CClientDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CClientDlg)
	// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	DDX_Control(pDX, IDC_SLIDER_QoE, Sl);
	DDX_Control(pDX, IDC_SLIDER1, slideLevel);
	DDX_Control(pDX, IDC_LIST, ListBox);
}

BEGIN_MESSAGE_MAP(CClientDlg, CDialog)
	//{{AFX_MSG_MAP(CClientDlg)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	//}}AFX_MSG_MAP

	ON_NOTIFY(NM_CUSTOMDRAW, IDC_SLIDER_QoE, &CClientDlg::OnNMCustomdrawSliderQoe)
	ON_LBN_SELCHANGE(IDC_LIST, &CClientDlg::OnLbnSelchangeList)
	ON_BN_CLICKED(IDC_BUTTON_UPDATELIST, &CClientDlg::OnBnClickedButtonUpdatelist)
	ON_NOTIFY(NM_CUSTOMDRAW, IDC_SLIDER1, &CClientDlg::OnNMCustomdrawSlider1)
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CClientDlg message handlers

BOOL CClientDlg::OnInitDialog()
{
	CDialog::OnInitDialog();

	// Add "About..." menu item to system menu.

	// IDM_ABOUTBOX must be in the system command range.
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		CString strAboutMenu;
		strAboutMenu.LoadString(IDS_ABOUTBOX);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}

	// Set the icon for this dialog.  The framework does this automatically
	//  when the application's main window is not a dialog
	SetIcon(m_hIcon, TRUE);			// Set big icon
	SetIcon(m_hIcon, FALSE);		// Set small icon
	
	// TODO: Add extra initialization here
	Sl.SetRange(0,10000000);
    Sl.SetTicFreq(100000);
	slideLevel.SetRange(0,4000000);
    slideLevel.SetTicFreq(4000000);  
	//ListBox


	return TRUE;  // return TRUE  unless you set the focus to a control
}

void CClientDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialog::OnSysCommand(nID, lParam);
	}
}

// If you add a minimize button to your dialog, you will need the code below
//  to draw the icon.  For MFC applications using the document/view model,
//  this is automatically done for you by the framework.

void CClientDlg::OnPaint() 
{
	if (IsIconic())
	{
		CPaintDC dc(this); // device context for painting

		SendMessage(WM_ICONERASEBKGND, (WPARAM) dc.GetSafeHdc(), 0);

		// Center icon in client rectangle
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// Draw the icon
		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialog::OnPaint();
	}
}

// The system calls this to obtain the cursor to display while the user drags
//  the minimized window.
HCURSOR CClientDlg::OnQueryDragIcon()
{
	return (HCURSOR) m_hIcon;
}
//Variable
CSocket aSocket;
int index;
struct application{
	CString name;
	CString protocol;
	CString srcip;
	CString dstip;
	CString srcport;
	CString dstport;
	int qoe;
	double level;
};
application app[100];

void CClientDlg::OnOK() 
{
	// TODO: Add extra validation here
/*	int defaultqoe;
	defaultqoe=5000000;
	Sl.SetPos(defaultqoe);
	int qoe=Sl.GetPos();*/

	AfxSocketInit();
	//CSocket aSocket;  4.22
	CString strIP;
	CString strPort;
	CString strText;
	CString strSrcIP;
	CString strDstIP;
	CString strSrcPort;
	CString strDstPort;
	CString strQOE;
	this->GetDlgItem(IDC_EDIT_IP)->GetWindowText(strIP);
	this->GetDlgItem(IDC_EDIT_PORT)->GetWindowText(strPort);
	this->GetDlgItem(IDC_EDIT_TEXT)->GetWindowText(strText);
	this->GetDlgItem(IDC_EDIT_SrcIP)->GetWindowText(strSrcIP);
	this->GetDlgItem(IDC_EDIT_DstIP)->GetWindowText(strDstIP);
	this->GetDlgItem(IDC_EDIT_SRCPORT)->GetWindowText(strSrcPort);
	this->GetDlgItem(IDC_EDIT_DSTPORT)->GetWindowText(strDstPort);
	this->GetDlgItem(IDC_EDIT_QOE)->GetWindowText(strQOE);
	 //Init Socket
	if(!aSocket.Create())
	{
		char szMsg[1024] = {0};

		sprintf(szMsg, "create faild: %d", aSocket.GetLastError());

		AfxMessageBox(szMsg);
		return;
	}
	//Calculate min bandwidth
	CString strQOEmin;
    strQOEmin.Format("%d",((Sl.GetPos()/10)*9));

	CString sendout;
	//sendout=strSrcIP+' '+strSrcPort+' '+strDstIP+' '+strDstPort;
	sendout="ovs-vsctl -- set port eth3 qos=@newqos  -- --id=@newqos create qos type=linux-htb other-config:max-rate=10000000 queues=0=@q0,1=@q1 -- --id=@q0 create Queue other-config:min-rate=10000 other-config:max-rate=10000000 other-config:priority=1  -- --id=@q1 create Queue other-config:min-rate="+strQOEmin+" other-config:max-rate="+strQOE+" other-config:priority=2"+"!"+"curl -d '{\"switch\": \"00:00:00:1b:21:62:55:b1\", \"name\":\"matchAppThroughUdpPort\", \"cookie\":\"0\",  \"priority\":\"1\",\"protocol\":\"17\", \"src-port\":\""+strSrcPort+"\", \"dst-port\":\""+strDstPort+"\", \"ether-type\":\"0x800\",\"dst-ip\":\""+strDstIP+"\",\"src-ip\":\""+strSrcIP+"\",\"active\":\"true\", \"actions\":\"enqueue=3:1\"}' http://192.168.12.178:8080/wm/staticflowentrypusher/json";
	int l=sendout.GetLength();
	CString len;
	len.Format("%d",l);
	sendout=len+' '+sendout;
	//transfer port kind
	int nPort = atoi(strPort);
	//Socket connect
	if(aSocket.Connect(strIP, nPort))
	{
		char szRecValue[1024] = {0};
		//send message to remote server
		aSocket.Send(sendout, sendout.GetLength());
		MessageBox(sendout);
		char length[100];
		sprintf(length,"%d",l);
		char c[100]="Message Sent ";
		strcat(c,length);
		AfxMessageBox(c);
		//Receive Information from server
//		aSocket.Receive((void *)szRecValue, 1024);
//		AfxMessageBox(szRecValue);
	}
	else
	{
		char szMsg[1024] = {0};
		sprintf(szMsg, "create faild: %d", aSocket.GetLastError());
		AfxMessageBox(szMsg);
	}

	aSocket.Close();
}
/*
void CClientDlg::OnCancel() 
{
	aSocket.Close();
}
*/



void CClientDlg::OnNMCustomdrawSliderQoe(NMHDR *pNMHDR, LRESULT *pResult)
{
	LPNMCUSTOMDRAW pNMCD = reinterpret_cast<LPNMCUSTOMDRAW>(pNMHDR);
	
	//Get Postion of Slider
	int pos = Sl.GetPos();
    //Edit show
    CString str="";
    str.Format("%d",pos);
    SetDlgItemText(IDC_EDIT_QOE,str);
	//QOE adjustment
	int n=ListBox.GetCurSel();
	if (n>=0) app[n].qoe=pos;

	/*
	int corres_level = 0;
	if (pos > 100000) corres_level = (1.33 * pos + 3000000);
	else corres_level = 32 * pos;
	slideLevel.SetPos(corres_level);
	*/

	*pResult = 0;
}


void CClientDlg::OnLbnSelchangeList()
{
	// TODO: Display
	CString strtext;
	CString strqoe;
	int nCurSel;   
    // Get current item in list  
    nCurSel = ListBox.GetCurSel();    
	// Get related Sub string   
    ListBox.GetText(nCurSel, strtext);       
    SetDlgItemText(IDC_EDIT_LISTBOX, strtext);
	SetDlgItemText(IDC_EDIT_PROTOCOL, app[nCurSel].protocol);
	SetDlgItemText(IDC_EDIT_SrcIP, app[nCurSel].srcip);
	SetDlgItemText(IDC_EDIT_SRCPORT, app[nCurSel].srcport);
	SetDlgItemText(IDC_EDIT_DstIP, app[nCurSel].dstip);
	SetDlgItemText(IDC_EDIT_DSTPORT, app[nCurSel].dstport);
    strqoe.Format("%d",app[nCurSel].qoe);
    SetDlgItemText(IDC_EDIT_QOE,strqoe);
	Sl.SetPos(app[nCurSel].qoe);
	slideLevel.SetPos(app[nCurSel].level);

}

CString * SplitString(CString str, char split, int& iSubStrs)
{
    //Position of Spliter
	int iPos = 0; 
	//Number of Spliter
    int iNums = 0; 
    CString strTemp = str;
    CString strRight;
    //Calculate number of Substring
    while (iPos != -1)
    {
        iPos = strTemp.Find(split);
        if (iPos == -1)
        {
            break;
        }
        strRight = strTemp.Mid(iPos + 1, str.GetLength());
        strTemp = strRight;
        iNums++;
    }
    if (iNums == 0) //No Spliter
    {
        //String = SubString condition
        iSubStrs = 1; 
        return NULL;
    }
    //Sub String Array
	// Sub String = Spilt charater number + 1
    iSubStrs = iNums + 1; 
    CString* pStrSplit;
    pStrSplit = new CString[iSubStrs];
    strTemp = str;
    CString strLeft;
    for (int i = 0; i < iNums; i++)
    {
        iPos = strTemp.Find(split);
        //Left string
        strLeft = strTemp.Left(iPos);
        //Right string
        strRight = strTemp.Mid(iPos + 1, strTemp.GetLength());
        strTemp = strRight;
        pStrSplit[i] = strLeft;
    }
    pStrSplit[iNums] = strTemp;
    return pStrSplit;
}

void CClientDlg::OnBnClickedButtonUpdatelist()
{
	system("python get5Tuples.py");
	// Update list
	ListBox.ResetContent();
	CStdioFile  file("./netstat.txt",CFile::modeRead);
	char buffer[1000];
	CString line;
	CString* str;
	CString* strsrc;
	CString* strdst;
	int iSubStrs;
	//Read String
	index=0;
	while(file.ReadString(line))
	{
		str=SplitString(line, ' ', iSubStrs);
		app[index].protocol=str[2];
		//AfxMessageBox(str[2]);
		strsrc=SplitString(str[6], ':', iSubStrs);
		//AfxMessageBox(str[6]);
		app[index].srcip=strsrc[0];
		app[index].srcport=strsrc[1];
		strdst=SplitString(str[12], ':', iSubStrs);
		//AfxMessageBox(str[12]);
		app[index].dstip=strdst[0];
		app[index].dstport=strdst[1];
		file.ReadString(app[index].name);
		//AfxMessageBox(app[index].name);
		index++;
	}

	//Close File
	file.Close();
	for(int i=0;i<index;++i) ListBox.AddString(_T(app[i].name));
	for(int i=0;i<index;++i) app[i].qoe=10000000;
	for(int i=0;i<index;++i) app[i].level=100;
	//ListBox.AddString(_T(" [vlc.exe]"));
}



void CClientDlg::OnNMCustomdrawSlider1(NMHDR *pNMHDR, LRESULT *pResult)
{
	LPNMCUSTOMDRAW pNMCD = reinterpret_cast<LPNMCUSTOMDRAW>(pNMHDR);

	int qoeLevel = slideLevel.GetPos();
	int corres_bandwidth = 0;
	// Adjust specific bandwidth according to QoE-Bandwidth Mapping of Online Video
	if (qoeLevel > 3130000) corres_bandwidth = ((qoeLevel - 3000000) / 1.33);
	else corres_bandwidth = qoeLevel / 32;

	Sl.SetPos(corres_bandwidth);

	//Get Slider Position
	int pos = Sl.GetPos(); 
    //CString str="";
    //str.Format("%d",pos);
    //SetDlgItemText(IDC_EDIT_QOE,str);
	int n=ListBox.GetCurSel();
	if (n>=0) app[n].qoe=pos;
	   
	*pResult = 0;
}
