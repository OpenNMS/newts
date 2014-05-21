Dependencies
------------

Install rubygems and the header files used by some of the gems:

    yum install ruby ruby-devel rubygems

Install bundler:

    gem install bundler

Install Jekyll and its dependencies:

    bundle install

Running Jekyll
--------------

To run Jekyll in a way that matches the GitHub Pages build server, use the following:

    bundle exec jekyll serve --watch --baseurl ''
