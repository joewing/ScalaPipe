
require 'buildr/scala'

ENV['USE_FSC']='yes'

repositories.remote << 'http://repo1.maven.org/maven2/'

Project.local_task :scaladoc
Project.local_task :laplace
Project.local_task :averaging
Project.local_task :averagingfpga
Project.local_task :temp
Project.local_task :search
Project.local_task :mandelbrot
Project.local_task :pi
Project.local_task :maze
Project.local_task :paper
Project.local_task :cluster
Project.local_task :radix
Project.local_task :loop
Project.local_task :nbody
Project.local_task :random
Project.local_task :lz77
Project.local_task :aes

Project.local_task :testopt

define 'ScalaPipe' do

   project.version = '0.0.1'
   package :jar
   compile.using :deprecation => true
   compile.using :other => '-unchecked'

   task :scaladoc do
      system 'mkdir -p doc && cd doc && scaladoc -doc-title ScalaPipe ../src/main/scala/autopipe/*.scala ../src/main/scala/autopipe/gen/*.scala ../src/main/scala/autopipe/dsl/*.scala ../src/main/scala/autopipe/opt/*.scala'
   end

   task :laplace => :compile do
      system 'scala -cp target/resources:target/classes examples.Laplace'
   end

   task :averaging => :compile do
      system 'scala -cp target/classes:target/resources examples.Averaging'
   end

   task :averagingfpga => :compile do
      system 'scala -cp target/classes:target/resources examples.AveragingFPGA'
   end

   task :temp => :compile do
      system 'scala -cp target/classes:target/resources examples.Temp'
   end

   task :search => :compile do
      system 'scala -cp target/classes:target/resources examples.Search'
   end

   task :mandelbrot => :compile do
      system 'scala -cp target/classes:target/resources examples.Mandelbrot'
   end

   task :pi => :compile do
      system 'scala -cp target/classes:target/resources examples.Pi'
   end

   task :maze => :compile do
      system 'scala -cp target/classes:target/resources examples.Maze'
   end

   task :paper => :compile do
      system 'scala -cp target/classes:target/resources examples.Paper'
   end

   task :cluster => :compile do
      system 'scala -cp target/classes:target/resources examples.Cluster'
   end

   task :radix => :compile do
      system 'scala -cp target/classes:target/resources examples.RadixSort'
   end

   task :loop => :compile do
      system 'scala -cp target/classes:target/resources examples.Loop'
   end

   task :nbody => :compile do
      system 'scala -cp target/classes:target/resources examples.NBody'
   end

   task :random => :compile do
      system 'scala -cp target/classes:target/resources examples.RandomTest'
   end

   task :lz77 => :compile do
      system 'scala -cp target/classes:target/resources examples.LZ77'
   end

   task :aes => :compile do
      system 'scala -cp target/classes:target/resources examples.AES'
   end

   task :testopt => :compile do
      system 'scala -cp target/classes:target/resources autopipe.opt.test.OptTest'
   end

end

