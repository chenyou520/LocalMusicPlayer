package com.chenyou.localmusicplayer.utils;

import com.chenyou.localmusicplayer.domain.Lyric;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 作用：解析歌词
 */
public class LyricUtils {
    private ArrayList<Lyric> lyrics;
    private boolean isExistLyric = false;

    /**
     * 是否存在歌词
     *
     * @return
     */
    public boolean isExistLyric() {
        return isExistLyric;
    }

    /**
     * 得到歌词列表
     *
     * @return
     */
    public ArrayList<Lyric> getLyrics() {
        return lyrics;
    }

    public void readLyricFile(File file) {
        if (file == null || !file.exists()) {
            //歌词文件不存在
            lyrics = null;
            isExistLyric = false;
        } else {
            //歌词文件存在
            lyrics = new ArrayList<>();
            isExistLyric = true;
            //取出歌词文件-一行一行的读取
            try {
                String line = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharset(file)));
                while ((line = reader.readLine()) != null) {
                    //1.解析歌词
                    line = parseLyric(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //2.歌词排序
            Collections.sort(lyrics, new Sort());
            //3.计算每句歌词高亮时间
            for (int i = 0; i < lyrics.size(); i++) {
                Lyric oneLyric = lyrics.get(i);
                if (i + 1 < lyrics.size()) {
                    Lyric twoLyric = lyrics.get(i + 1);
                    oneLyric.setSleepTime(twoLyric.getTimePoint() - oneLyric.getTimePoint());
                }
            }
        }
    }

    /**
     * 解析歌词
     *
     * @param line
     * @return
     */
    private String parseLyric(String line) {
        int post1 = line.indexOf("[");//0,没有就返回-1
        int post2 = line.indexOf("]");//9,没有就返回-1

        if (post1 == 0 & post2 != -1) {

            long[] timePoints = new long[getCountTag(line)];//111,222,333
            String strTime = line.substring(post1 + 1, post2);//02:04.12
            timePoints[0] = strtime2long(strTime);//02:04.12-->long毫秒
            if (timePoints[0] == -1) {
                return "";
            }

            int i = 1;
            String content = line;//[02:04.12][03:37.32][00:59.73]我在这里欢笑
            while (post1 == 0 & post2 != -1) {
                content = content.substring(post2 + 1);//[03:37.32][00:59.73]我在这里欢笑-->[00:59.73]我在这里欢笑-->我在这里欢笑
                post1 = content.indexOf("[");//0
                post2 = content.indexOf("]");//9
                if (post2 != -1) {
                    strTime = content.substring(post1 + 1, post2);//03:37.32-->00:59.73
                    timePoints[i] = strtime2long(strTime);

                    if (timePoints[i] == -1) {
                        return "";
                    }

                    i++;

                }

            }

            Lyric lyric = new Lyric();
            for (int j = 0; j < timePoints.length; j++) {

                if (timePoints[j] != 0) {
                    lyric.setContent(content);
                    lyric.setTimePoint(timePoints[j]);
                    lyrics.add(lyric);
                    lyric = new Lyric();
                }
            }

            return content;
        }
        return null;
    }

    /**
     * 02:04.12--->转换成毫秒
     *
     * @param strTime
     * @return
     */
    private long strtime2long(String strTime) {
        long result = -1;
        try {
            //1.根据:把02:04.12切割成 02和04.12
            //2.根据.把04.12切换成04和12
            String s1[] = strTime.split(":");
            String s2[] = s1[1].split("\\.");

            //分
            long min = Long.parseLong(s1[0]);//02

            //秒
            long second = Long.parseLong(s2[0]);//04

            //毫秒
            long mil = Long.parseLong(s2[1]);//12;


            result = min * 60 * 1000 + second * 1000 + mil * 10;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * 判断文件编码
     *
     * @param file 文件
     * @return 编码：GBK,UTF-8,UTF-16LE
     */
    private String getCharset(File file) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF)
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    class Sort implements Comparator<Lyric> {
        @Override
        public int compare(Lyric lhs, Lyric rhs) {
            if (lhs.getTimePoint() < rhs.getTimePoint()) {
                return -1;
            } else if (lhs.getTimePoint() > rhs.getTimePoint()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 判断有多少句歌词
     *
     * @param line [02:04.12][03:37.32][00:59.73]我在这里欢笑
     * @return
     */
    private int getCountTag(String line) {
        int result = -1;
        String[] left = line.split("\\[");
        String[] right = line.split("\\]");
        if (left.length == 0 && right.length == 0) {
            result = 1;
        } else if (left.length > right.length) {
            result = left.length;
        } else {
            result = right.length;
        }
        return result;
    }
}
