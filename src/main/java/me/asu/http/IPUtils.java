/*
 * Copyright (C) 2017 Bruce Asu<bruceasu@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions:
 *  　　
 * 　　The above copyright notice and this permission notice shall
 * be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package me.asu.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bruce on 2015/10/26/026.
 */
public abstract class IPUtils {
    static Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");

//    public static String getPublicIP(String ...urls) {
//        for (String u : urls) {
//            String ip = getIp(u);
//            if (!Strings.isBlank(ip)) {
//                return ip;
//            }
//        }
//        return null;
//    }

//
//    public static String getIp(String u) {
//        try {
//            Sender sender = Sender.create(u);
//            Response resp = sender.send();
//            String content = resp.getContent();
//            Matcher m = pattern.matcher(content);
//            if (m.find()) {
//                return m.group(0);
//            }
//        } catch (HttpException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    // 将127.0.0.1形式的IP地址转换成十进制整数，这里没有进行任何错误处理
    public static int ipToInt(String strIp) {
        int[] ip = new int[4];
        // 先找到IP地址字符串中.的位置
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);
        // 将每个.之间的字符串转换成整型
        ip[0] = Integer.parseInt(strIp.substring(0, position1));
        ip[1] = Integer.parseInt(strIp.substring(position1 + 1, position2));
        ip[2] = Integer.parseInt(strIp.substring(position2 + 1, position3));
        ip[3] =Integer.parseInt(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    // 将十进制整数形式转换成127.0.0.1形式的ip地址
    public static String intToIp(int intIp) {
        StringBuffer sb = new StringBuffer("");
        // 直接右移24位
        sb.append(String.valueOf((intIp >>> 24)));
        sb.append(".");
        // 将高8位置0，然后右移16位
        sb.append(String.valueOf((intIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        // 将高16位置0，然后右移8位
        sb.append(String.valueOf((intIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        // 将高24位置0
        sb.append(String.valueOf((intIp & 0x000000FF)));
        return sb.toString();
    }

    public static void main(String[] args) {
//        String[] urls = new String[]{"http://www.ip138.com/ip2city.asp","http://www.whereismyip.com/"};
//        System.out.println(getPublicIP(urls));
        System.out.println(ipToInt("127.0.0.1"));
        System.out.println(intToIp(42353534));
    }
}
/*
def getip(self):
        try:
            myip = self.visit("http://www.ip138.com/ip2city.asp")
        except Exception:
            try:
                myip = self.visit("http://www.whereismyip.com/")
            except:
                myip = "So sorry!!!"
        return myip

    def visit(self,url):
        opener = urllib.request.urlopen(url)
        str = opener.read()
        opener.close()
        pattern = re.compile(r'\d+\.\d+\.\d+\.\d+')
        return pattern.search(str.decode('gbk')).group(0)
getmyip = Getmyip()
localip = getmyip.getip()
company_ip1="120.197.96.162\n"
company_ip2="120.237.114.83\n"
print(localip)
out = open('trust.list', 'w')
out.write(company_ip1)
out.write(company_ip2)
out.write(localip)
out.close()
print('DONE!')
 */