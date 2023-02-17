Tubes 1 Strategi Algoritma Kelompok 34 breve
===
# Tugas Besar 1 IF 2211 Strategi Algoritma Algoritma Greedy pada Galaxio
**_Program Ini Dibuat Untuk Memenuhi Tugas Perkuliahan Mata Kuliah Strategi Algoritma (IF2211)_**
<p align="center">
Prodi Teknik Informatika <br/>
Sekolah Teknik Elektro dan Informatika<br/>
Institut Teknologi Bandung<br/>
Semester 2 Tahun 2022/2023<br/>
</p>

## Deskripsi
Tugas besar pertama IF2211 - Strategi Algoritma menugaskan mahasiswa untuk merancang suatu algoritma greedy dalam permainan Galaxio. Permainan Galaxio sendiri merupakan permainan yang ditandingkan pada Entellect Challenge 2021. Algoritma greedy yang diimplementasikan oleh kelompok 34 breve adalah algoritma yang menggunakan pendekatan defensif tetapi seagresif mungkin. Dengan algoritma yang diimplementasikan, bot berusaha memiliki ukuran terbesar yang mungkin sambil terus-menerus menyerang lawan bila memungkinkan. Selain itu, bot akan berusaha untuk bertahan hidup bila mendeteksi ancaman.

## Struktur Folder
1. `doc` berisi dokumen dari tugas besar
2. `src` berisi source code dari tugas besar dalam bahasa pemrograman Java
3. `target` berisi hasil build dari source code termasuk file executable .jar

## Program Environment
- Java(TM) SE Runtime Environment (build 17.0.4.1+1-LTS-2)
- java version "17.0.4.1" 2022-08-18 LTS
- .NET Core 3.1 SDK (v3.1.407) - Windowsx64 (untuk sistem operasi lain menyesuaikan)
- Apache Maven 3.9.0 (untuk build source code)
- Visual Studio Code dan IntelliJ IDEA (untuk pengembangan source code)

## Cara Menjalankan Program
1. Unduh starter pack yang bisa didapatkan dari [tautan berikut](https://github.com/EntelectChallenge/2021-Galaxio/releases/tag/2021.3.2)
2. Ubah pengaturan permainan sesuai kemauan pada berkas `appsettings.json` dalam folder `runner-publish` dan `engine-publish` dalam `starter-pack`
3. Jalankan runner pada `runner-publish` dengan perintah `dotnet GameRunner.dll`
4. Jalankan engine pada `engine-publish` dengan perintah `dotnet Engine.dll`
5. Jalankan logger pada `logger-publish` dengan perintah `dotnet Logger.dll`
6. Jalankan seluruh bot yang ingin dimainkan (apabila reference bot yang ingin dimainkan maka jalankan perintah `dotnet ReferenceBot.dll` pada folder `reference-bot-publish`, bila bot hasil build dari tugas besar ini maka jalankan perintah `java -jar Breve.jar` pada folder `target` pada direktori tugas besar ini
7. Setelah permainan selesai, muat berkas hasil logger pada visualiser yang terdapat pada folder `visualiser` di dalam `starter-pack`

## Anggota
<table>
    <tr>
      <td><b>Nama</b></td>
      <td><b>NIM</b></td>
    </tr>
    <tr>
      <td><b>Ammar Rasyad Chaeroel</b></td>
      <td><b>13521136</b></td>
    </tr>
    <tr>
      <td><b>Edia Zaki Naufal Ilman</b></td>
      <td><b>13521141</b></td>
    </tr>
    <tr>
      <td><b>Bintang Dwi Marthen</b></td>
      <td><b>13521144</b></td>
    </tr>
</table>
