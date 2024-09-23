import pandas as pd
import matplotlib.pyplot as plt
import numpy as np


def read_main_csv(file_path):
    df = pd.read_csv(file_path)
    time = df['Time (s)']
    temp = df['TEMP']
    return time, temp


def read_secondary_csv(file_path):
    df = pd.read_csv(file_path, header=None)
    return df[0]


# Main function to plot the graphs
def plot_graphs(sim_hotter, sim_colder, firevox_hotter, firevox_colder):
    # Read data from the main CSV
    time, sim_hotter_temp = read_main_csv(sim_hotter)
    time, sim_colder_temp = read_main_csv(sim_colder)
    firevox_hotter_temp = read_secondary_csv(firevox_hotter)
    firevox_colder_temp = read_secondary_csv(firevox_colder)

    # Read secondary data
    firevox_time = [0.1 * x for x in range(0, 1001)]

    # Plot Time vs. Inner Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, sim_hotter_temp, label='SimScale', color='green')
    plt.plot(firevox_time, firevox_hotter_temp, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title('Slab temperature at hotter (15, 15, 0) cm')
    plt.legend()
    plt.grid(True)
    plt.show()

    # Plot Time vs. Gas Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, sim_colder_temp, label='SimScale', color='green')
    plt.plot(firevox_time, firevox_colder_temp, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title('Slab temperature at colder (15, 15, 120) cm')
    plt.legend()
    plt.grid(True)
    plt.show()

# Example usage
sim_hotter = "conduction/hotter.csv"
sim_colder = "conduction/colder.csv"
firevox_hotter = "conduction/thermometerHotterSide.csv"
firevox_colder = "conduction/thermometerColderSide.csv"

plot_graphs(sim_hotter, sim_colder, firevox_hotter, firevox_colder)
