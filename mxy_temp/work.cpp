#include<iostream>
#include<string>
#include<cstring>
#include<time.h>
#include<cstdio>
using namespace std;
char st[1000];
long long  sensor[10][10];
const int sensor_size[]={2,3,4,4,3,3,4,4,3,2};
const char format[] = "btspp://%s:1;authenticate=false;encrypt=false;master=false\tB%dS%d\t%lld\n";
string switch_btsapp(const string &st){
    if (st.find("Red"   )!=string::npos) return "00066661A594";
    if (st.find("Blue"  )!=string::npos) return "00066661A3E4";
    if (st.find("Yellow")!=string::npos) return "00066661A290";
    if (st.find("Pink"  )!=string::npos) return "00066661A234";
    if (st.find("Orange")!=string::npos) return "00066661AD56";
    if (st.find("Silver")!=string::npos) return "0006664989CE";
    if (st.find("Green" )!=string::npos) return "000666498A92";
    if (st.find("Black" )!=string::npos) return "000666619AE2";
    return "XXXXXXXXXXXX";
}
int main(){
    int s,b;
    long long t;    

    int ls,lb;
    long long lt;
    
    string btsapp="XXXXXXXXXXXX";

    bool valid=0;
    while(cin.getline(st,1000)){
        string ss=st;
        if (ss.find("[")==string::npos){
            btsapp = switch_btsapp(ss);
            break;
        }
        if (ss.find("Timeout")!=string::npos){
            valid=0;
            continue;
        }
        string s1;
        
        s1=ss.substr(ss.find("DETECT"),ss.find("Car"));
        btsapp = switch_btsapp(s1);

        s1=ss.substr(0,ss.find("]"));
        sscanf(s1.c_str(),"[B%dS%d]",&s,&b);

        s1=ss.substr(ss.find(":")+1,ss.size());
        sscanf(s1.c_str(),"%lld",&t);
    
        //cout<<s<<" "<<b<<" "<<t<<endl;
        if (valid){
            sensor[ls][lb]=max(sensor[ls][lb],t-lt);
        }
        valid=1;
        ls=s;lb=b;lt=t;
    }
    for(int i=0;i<10;i++){
        for(int j=1;j<=sensor_size[i];j++){
            printf(format,btsapp.c_str(),i,j,sensor[i][j]? sensor[i][j]+400:5000);
        }
    }
    return 0;
}