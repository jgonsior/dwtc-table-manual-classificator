from setuptools import setup

# include requirements from requirements.txt
with open('requirements.txt') as f:
    requirements = f.read().splitlines()

setup(
    name='tableBrowser',
    packages=['tableBrowser'],
    include_package_data=True,
    install_requires=requirements
)
